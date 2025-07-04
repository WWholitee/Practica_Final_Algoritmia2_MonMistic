package com.example.monmistic;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
// import android.graphics.Rect; // REMOVED
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    //////INICIALIZACIONES
    public Button BMapa, BCriatura, BInventario, Bmenos1, Bmenos2, Bmas1, Bmas2;
    private TextView textViewNomZona;
    private TextView textViewPuntos;
    private TextView textCriatures;
    private int puntos;
    private EditText editTextCercaZona;
    private ArrayAdapter<String> suggestionsAdapter;
    private ListView suggestionsListView;

    private TextView textViewZoom;
    private SurfaceView SV, SV2;

    String contingutJSON;
    UnsortedLinkedListSet<View> mapaViews;
    UnsortedLinkedListSet<View> craituresViews;
    UnsortedLinkedListSet<View> inventariViews;
    private String conjuntoVisible = "";
    Map<String, String> reglesDeJoc;



    private ZonaTrie catalegZonesTrie;
    TreeMap<String, Zona> catalegZonesMapa = new TreeMap<>();

    // Nivell 1:    Clau: Zona      Valor: HashMap <String, List<Criatura>>
    // Nivell 2:    Clau: Gènere    Valor: HashSet a criatures per recuperació ràpida de les dades
    private TreeMap<String, Map<Genere, HashSet<Criatura>>> catalegCriatures;
    private TreeMap<Criatura, Zona> criaturesCapturades;
    private TreeMap<Criatura, Zona> criaturesEscapades;


    private Context context;
    private Bitmap bmp;
    private float Cx, Cy;
    private float fe;
    private float zoomMinim;
    private float zoomMaxim;
    private float incrementZoom;

    ////seguimiento de los dedos
    private float cursorX,cursorY;
    private boolean Moviendo = false;

    private ScaleGestureDetector scaleDetector;
    private boolean Escalando = false;

    private CountDownTimer zoneUpdateTimer;
    private Dialog currentCombatDialog = null;
    private RadioButton currentlySelectedRadioButton = null;


    private final int DISTANCIA_ESCAPE = 200;
    private final int DISTANCIA_COMBAT = 30;

    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        catalegZonesTrie = new ZonaTrie();

        inicializarConjuntoMapa();
        inicializarConjuntoCriaturas();
        inicializarConjuntoInventario();
        inicialitzarReglesJoc();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BMapa = findViewById(R.id.botonMapa);
        BCriatura = findViewById(R.id.botonCriatura);
        BInventario = findViewById(R.id.botonInventario);

        context = getApplicationContext();
        SV = findViewById(R.id.surfaceView);

        BitmapFactory.Options opcions = new BitmapFactory.Options () ;
        opcions.inScaled = false ;

        bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.mapam, opcions);

        contingutJSON = llegirJSON(context, R.raw.zones);
        crearEstructuresDeDadesDeZones(contingutJSON);
        generarCriatures();

        SV.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                zoomMinim = (float) SV.getHeight() / bmp.getHeight();
                zoomMaxim = zoomMinim * 10;
                incrementZoom = zoomMaxim / 50;

                Cx = bmp.getWidth() / 2f;
                Cy = bmp.getHeight() / 2f;

                fe = zoomMaxim/2;

                dibuixaMapa();
                actualitzaInformacioZonaActual();

                iniciarTemporitzadorActualitzacioZona();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                if (zoneUpdateTimer != null) {
                    zoneUpdateTimer.cancel();
                }
            }
        });

        BMapa.setOnClickListener(v -> {
            visibilizar("mapa");
            iniciarTemporitzadorActualitzacioZona();
        });

        BCriatura.setOnClickListener(v -> {
            visibilizar("criatures");
            if (zoneUpdateTimer != null) {
                zoneUpdateTimer.cancel();
            }
        });

        BInventario.setOnClickListener(v -> {
            visibilizar("inventari");
            if (zoneUpdateTimer != null) {
                zoneUpdateTimer.cancel();
                mostrarInventari();
            }
        });

        suggestionsListView = findViewById(R.id.ListView);
        scaleDetector = new ScaleGestureDetector(this,new ScaleListener());

        suggestionsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, // Layout estàndard per a cada element de la llista
                new ArrayList<>()); // Llista de suggeriments inicialment buida
        suggestionsListView.setAdapter(suggestionsAdapter);

        editTextCercaZona.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String prefix = s.toString();
                if (prefix.isEmpty()) {
                    suggestionsAdapter.clear();
                    suggestionsListView.setVisibility(View.GONE); // Amagar la llista si el text està buit
                    return;
                }

                // Obté els suggeriments del Trie basats en el prefix actual
                List<Zona> suggestedZones = catalegZonesTrie.getSuggestions(prefix);
                List<String> suggestedNames = new ArrayList<>();
                for (Zona zona : suggestedZones) {
                    // Afegeix el nom de la zona que vols que es mostri a la llista de suggeriments
                    suggestedNames.add(zona.getNomPopular()); // O zona.getNomOficial() si és el que uses
                }

                // Actualitza l'adaptador del ListView amb els nous suggeriments
                suggestionsAdapter.clear();
                suggestionsAdapter.addAll(suggestedNames);
                suggestionsAdapter.notifyDataSetChanged(); // Notifica els canvis a l'adaptador

                // Mostra la llista de suggeriments si n'hi ha, altrament, amaga-la
                if (!suggestedNames.isEmpty()) {
                    suggestionsListView.setVisibility(View.VISIBLE);
                } else {
                    suggestionsListView.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        suggestionsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedZoneName = (String) parent.getItemAtPosition(position);
            editTextCercaZona.setText(selectedZoneName); // Omple l'EditText amb el suggeriment seleccionat
            // Posa el cursor al final del text, per si l'usuari vol seguir editant
            editTextCercaZona.setSelection(editTextCercaZona.getText().length());

            // Amaga la llista de suggeriments un cop s'ha seleccionat un
            suggestionsListView.setVisibility(View.GONE);

            // Després de seleccionar un suggeriment, pots voler activar la lògica de cerca immediatament
            performZoneSearch(selectedZoneName); // Crida el mètode que conté la lògica de cerca
        });
    }

    public void inicializarConjuntoMapa() {
        SurfaceView sv = findViewById(R.id.surfaceView);
        textViewNomZona = findViewById(R.id.NomZONA);
        textViewPuntos = findViewById(R.id.Puntos);
        editTextCercaZona = findViewById(R.id.CercaZona);

        textViewZoom = findViewById(R.id.PorcentajeZoom);
        Bmas1 = findViewById(R.id.bottonmas1);
        Bmenos1 = findViewById(R.id.bottonmenos1);
        Bmas2 = findViewById(R.id.bottonmas2);
        Bmenos2 = findViewById(R.id.bottonmenos2);

        mapaViews = new UnsortedLinkedListSet<View>();
        mapaViews.add(sv);
        mapaViews.add(textViewNomZona);
        mapaViews.add(textViewPuntos);
        mapaViews.add(editTextCercaZona);
        mapaViews.add(textViewZoom);
        mapaViews.add(Bmas1);
        mapaViews.add(Bmenos1);
        mapaViews.add(Bmas2);
        mapaViews.add(Bmenos2);

        Bmenos2.setOnClickListener(v -> {
            ferZoomOut();
            actualitzaInformacioZonaActual();
        });
        Bmas2.setOnClickListener(v -> {
            ferZoomIn();
            actualitzaInformacioZonaActual();
        });
        Bmenos1.setOnClickListener(v -> {
            fe = zoomMinim;
            dibuixaMapa();
            actualitzaInformacioZonaActual();
        });

        Bmas1.setOnClickListener(v -> {
            fe = zoomMaxim;
            dibuixaMapa();
            actualitzaInformacioZonaActual();
        });

        editTextCercaZona.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                String nomBuscado = editTextCercaZona.getText().toString();
                Zona zonaTrobadad = catalegZonesTrie.search(nomBuscado);
                if (zonaTrobadad != null) {
                    float zonaCenterX = (zonaTrobadad.getX1() + zonaTrobadad.getX2()) / 2f;
                    float zonaCenterY = (zonaTrobadad.getY1() + zonaTrobadad.getY2()) / 2f;

                    Cx = zonaCenterX;
                    Cy = zonaCenterY;

                    float zonaWidth = zonaTrobadad.getX2() - zonaTrobadad.getX1();
                    float zonaHeight = zonaTrobadad.getY2() - zonaTrobadad.getY1();

                    float scaleX = (float) SV.getWidth() / zonaWidth;
                    float scaleY = (float) SV.getHeight() / zonaHeight;
                    fe = Math.min(scaleX, scaleY) * 0.9f;

                    fe = Math.max(zoomMinim, Math.min(fe, zoomMaxim));

                    dibuixaMapa();
                    actualitzaInformacioZonaActual();
                    Toast.makeText(context, "Zona trobada: " + zonaTrobadad.getNomOficial(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Zona no trobada: " + nomBuscado, Toast.LENGTH_SHORT).show();
                }
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editTextCercaZona.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }

    private void performZoneSearch(String nomBuscado) {
        Zona zonaTrobadad = catalegZonesTrie.search(nomBuscado); // Cerca exacta amb el Trie
        if (zonaTrobadad != null) {
            float zonaCenterX = (zonaTrobadad.getX1() + zonaTrobadad.getX2()) / 2f;
            float zonaCenterY = (zonaTrobadad.getY1() + zonaTrobadad.getY2()) / 2f;

            Cx = zonaCenterX;
            Cy = zonaCenterY;

            float zonaWidth = zonaTrobadad.getX2() - zonaTrobadad.getX1();
            float zonaHeight = zonaTrobadad.getY2() - zonaTrobadad.getY1();

            if (SV != null) {
                float scaleX = (float) SV.getWidth() / zonaWidth;
                float scaleY = (float) SV.getHeight() / zonaHeight;
                fe = Math.min(scaleX, scaleY) * 0.9f;
            } else {
                Log.e("MainActivity", "SV (SurfaceView) és nul. No es pot calcular l'escala.");
                fe = 1.0f; // Valor per defecte segur
            }

            fe = Math.max(zoomMinim, Math.min(fe, zoomMaxim));

            // Crides als teus mètodes de dibuix i actualització del mapa
            dibuixaMapa(); // Descomenta aquestes línies un cop tinguis els mètodes
            // actualitzaInformacioZonaActual(); // Descomenta aquestes línies un cop tinguis els mètodes
            Toast.makeText(this, "Zona trobada: " + zonaTrobadad.getNomOficial(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Zona no trobada: " + nomBuscado, Toast.LENGTH_SHORT).show();
        }
        // Amaga el teclat virtual
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editTextCercaZona.getWindowToken(), 0);

        // Amaga també la llista de suggeriments després de la cerca o quan el teclat s'amaga
        suggestionsListView.setVisibility(View.GONE);
    }
    public void inicializarConjuntoCriaturas(){
        SurfaceView sv2 = findViewById(R.id.surfaceView2);
        textCriatures = findViewById(R.id.textCriaturas);
        RadioGroup radioGroup = findViewById(R.id.radioGroupCriatures);
        RadioButton rbCriaturesZona = findViewById(R.id.radioButtonCriaturesZona);
        RadioButton rbZonesMapa = findViewById(R.id.radioButtonZonesMapa);
        RadioButton rbCapturades = findViewById(R.id.radioButtonCapturades);
        RadioButton rbEscapades = findViewById(R.id.radioButtonEscapades);

        craituresViews = new UnsortedLinkedListSet<View>();
        craituresViews.add(sv2);
        craituresViews.add(textCriatures);
        craituresViews.add(radioGroup);
        craituresViews.add(rbCriaturesZona);
        craituresViews.add(rbZonesMapa);
        craituresViews.add(rbCapturades);
        craituresViews.add(rbEscapades);

        View.OnClickListener radioButtonClickListener = v -> {
            RadioButton clickedRadioButton = (RadioButton) v;

            if (clickedRadioButton == currentlySelectedRadioButton) {
                // Si se hace clic en el ya seleccionado, deseleccionar
                radioGroup.clearCheck();
                currentlySelectedRadioButton = null;
                textCriatures.setText(""); // Limpiar el texto
            } else {
                // Seleccionar el nuevo RadioButton
                currentlySelectedRadioButton = clickedRadioButton;
                clickedRadioButton.setChecked(true);

                // Mostrar el contenido correspondiente
                if (clickedRadioButton == rbCriaturesZona) {
                    mostrarCriaturesPerZona();
                } else if (clickedRadioButton == rbZonesMapa) {
                    mostrarZonesDelMapa();
                } else if (clickedRadioButton == rbCapturades) {
                    mostrarCriaturesCapturades();
                } else if (clickedRadioButton == rbEscapades) {
                    mostrarCriaturesEscapades();
                }
            }
        };

        // Asignar el listener a cada RadioButton
        rbCriaturesZona.setOnClickListener(radioButtonClickListener);
        rbZonesMapa.setOnClickListener(radioButtonClickListener);
        rbCapturades.setOnClickListener(radioButtonClickListener);
        rbEscapades.setOnClickListener(radioButtonClickListener);

        // Listener para detectar cambios automáticos (por si acaso)
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Este listener se mantiene pero no es estrictamente necesario
        });



        // Habilitar scrolling en el TextView
        textCriatures.setMovementMethod(new ScrollingMovementMethod());
    }

    public void inicializarConjuntoInventario(){
        SurfaceView svInventari = findViewById(R.id.surfaceView3); // Cambiado a svInventari para claridad

        inventariViews = new UnsortedLinkedListSet<View>();
        inventariViews.add(svInventari);

        svInventari.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                mostrarInventari();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {}
        });


    }
    private void mostrarInventari() {
        SurfaceView svInventari = findViewById(R.id.surfaceView3);

        if (svInventari.getHolder().getSurface().isValid()) {
            Canvas canvas = svInventari.getHolder().lockCanvas();
            try {
                // Limpiar el canvas
                canvas.drawColor(Color.WHITE);

                // Configuración inicial
                int padding = 20;
                int screenWidth = svInventari.getWidth();
                int screenHeight = svInventari.getHeight();
                int itemSize = (screenWidth - (padding * 9)) / 8; // 8 imágenes + márgenes
                itemSize = Math.min(itemSize, (screenHeight - padding * 5) / 4); // Ajustar por altura


                int currentX = padding;
                int currentY = padding;
                Bitmap check = BitmapFactory.decodeResource(getResources(), R.drawable.check);

                // Ordenar géneros alfabéticamente
                TreeMap<String, Genere> generesOrdenats = new TreeMap<>();
                for (Genere g : getGeneresUnics()) {
                    generesOrdenats.put(g.getName(), g);
                }

                // Pintar cada género y sus especies
                for (Map.Entry<String, Genere> entry : generesOrdenats.entrySet()) {
                    Genere genere = entry.getValue();

                    // Título del género
                    Paint paint = new Paint();
                    paint.setColor(Color.BLACK);
                    paint.setTextSize(30);
                    canvas.drawText(genere.getName().toUpperCase(), currentX, currentY - 5, paint);
                    currentY += 35;

                    // Pintar especies (1-8)
                    for (int especie = 1; especie <= 8; especie++) {
                        if (currentX + itemSize > svInventari.getWidth()) {
                            currentX = padding;
                            currentY += itemSize + padding;
                        }

                        // Obtener imagen de la criatura
                        String imageName = genere.getName().toLowerCase() + especie;
                        int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());

                        if (resId != 0) {
                            Bitmap bmpCriatura = BitmapFactory.decodeResource(getResources(), resId);
                            Rect dst = new Rect(currentX, currentY, currentX + itemSize, currentY + itemSize);
                            canvas.drawBitmap(bmpCriatura, null, dst, null);

                            // Superponer check si está capturada
                            if (esCriaturaCapturada(genere, especie)) {
                                Rect checkDst = new Rect(
                                        currentX + itemSize - 40,
                                        currentY + itemSize - 40,
                                        currentX + itemSize,
                                        currentY + itemSize
                                );
                                canvas.drawBitmap(check, null, checkDst, null);
                            }
                        }

                        currentX += itemSize + padding;
                    }

                    currentX = padding;
                    currentY += itemSize + padding + 15;
                }
            } finally {
                if (canvas != null) {
                    svInventari.getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    // Métodos auxiliares
    private Set<Genere> getGeneresUnics() {
        Set<Genere> generes = new HashSet<>();

        // Recorrer todas las zonas del catálogo
        for (Map<Genere, HashSet<Criatura>> criaturesPerGenere : catalegCriatures.values()) {
            // Añadir todos los géneros encontrados en esta zona
            generes.addAll(criaturesPerGenere.keySet());
        }

        return generes;
    }
//    private void mostrarInventari() {
//        SurfaceView svInventari = findViewById(R.id.surfaceView3);
//
//        if (svInventari.getHolder().getSurface().isValid()) {
//            Canvas canvas = svInventari.getHolder().lockCanvas();
//            try {
//                // Limpiar el canvas
//                canvas.drawColor(Color.WHITE);
//
//                // Configuración de dimensiones
//                int padding = 20;
//                int screenWidth = svInventari.getWidth();
//                int screenHeight = svInventari.getHeight();
//
//                // Calcular tamaño de imágenes para que quepan 8 por fila
//                int itemSize = (screenWidth - (padding * 9)) / 8; // 8 imágenes + márgenes
//                itemSize = Math.min(itemSize, (screenHeight - padding * 5) / 4); // Ajustar por altura
//
//                // Precargar el check
//                Bitmap check = BitmapFactory.decodeResource(getResources(), R.drawable.check);
//                check = Bitmap.createScaledBitmap(check, itemSize, itemSize, true);
//
//                // Coordenadas iniciales
//                int currentY = padding;
//
//                // Lista ordenada de géneros
//                Genere[] generes = {
//                        new Genere("aiguard", 0.01f, Color.MAGENTA, 10, 0),
//                        new Genere("focguard", 0.015f, Color.GREEN, 15, 1),
//                        new Genere("tornadrac", 0.02f, Color.RED, 20, 2.5f),
//                        new Genere("vapordrac", 0.025f, Color.BLUE, 30, 3.5f)
//                };
//
//                // Pintar cada género en su fila
//                for (Genere genere : generes) {
//                    int currentX = padding;
//
//                    // Título del género
//                    Paint paint = new Paint();
//                    paint.setColor(Color.BLACK);
//                    paint.setTextSize(24);
//                    //canvas.drawText(genere.getName().toUpperCase(), currentX, currentY - 5, paint);
//
//                    // Pintar las 8 especies en línea horizontal
//                    for (int especie = 1; especie <= 8; especie++) {
//                        String imageName = genere.getName().toLowerCase() + especie;
//                        int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());
//
//                        if (resId != 0) {
//                            Bitmap bmpCriatura = BitmapFactory.decodeResource(getResources(), resId);
//                            Rect dst = new Rect(currentX, currentY, currentX + itemSize, currentY + itemSize);
//
//                            // Dibujar la criatura
//                            Paint paintCriatura = new Paint();
//                            if (!esCriaturaCapturada(genere, especie)) {
//                                paintCriatura.setAlpha(128); // 50% transparencia si no está capturada
//                            }
//                            canvas.drawBitmap(bmpCriatura, null, dst, null);
//
//                            // Dibujar check si está capturada
//                            if (esCriaturaCapturada(genere, especie)) {
//                                Log.d("DIBUJAR_CHECK", "Dibujando check para: " + genere.getName() + especie);
//
//                                // Crear un Paint con transparencia para el check (opcional)
//                                Paint checkPaint = new Paint();
//                                checkPaint.setAlpha(180); // Semi-transparente
//
//                                canvas.drawBitmap(check, null, dst, null);
//                            }
//                        }
//
//                        currentX += itemSize + padding;
//                    }
//
//                    currentY += itemSize + padding;
//                }
//
//            } finally {
//                if (canvas != null) {
//                    svInventari.getHolder().unlockCanvasAndPost(canvas);
//                }
//            }
//        }
//    }
//
//    // Métodos auxiliares (igual que los tuyos)
//    private Set<Genere> getGeneresUnics() {
//        Set<Genere> generes = new HashSet<>();
//        for (Criatura c : criaturesCapturades.keySet()) {
//            generes.add(c.getGenere());
//        }
//        return generes;
//    }

    private boolean esCriaturaCapturada(Genere genere, int especie) {
        for (Criatura c : criaturesCapturades.keySet()) {
            if (c.getGenere().equals(genere) && c.getEspecie() == especie) {
                return true;
            }
        }
        return false;
    }
    private void visibilizar(String conjunto){
        for (View view : mapaViews     ) view.setVisibility(View.INVISIBLE);
        for (View view : craituresViews) view.setVisibility(View.INVISIBLE);
        for (View view : inventariViews) view.setVisibility(View.INVISIBLE);

        if(conjunto.equals(conjuntoVisible)){
            conjuntoVisible = "";
        } else {
            switch (conjunto){
                case "mapa":
                    for (View view : mapaViews) view.setVisibility(View.VISIBLE);
                    conjuntoVisible = "mapa";
                    break;
                case "criatures":
                    for (View view : craituresViews) view.setVisibility(View.VISIBLE);
                    conjuntoVisible = "criatures";
                    break;
                case "inventari":
                    for (View view : inventariViews) view.setVisibility(View.VISIBLE);
                    conjuntoVisible = "inventari";
                    break;
            }
        }
    }

    private void inicialitzarReglesJoc() {
        reglesDeJoc = new HashMap<>();
        reglesDeJoc.put("pedra", "tisores");
        reglesDeJoc.put("tisores", "paper");
        reglesDeJoc.put("paper", "pedra");
    }

    /**
     * Genera 500 criatures i les distribueix aleatòriament per les zones.
     * S'assignen 125 criatures de cada gènere.
     */
    private void generarCriatures(){
        // Inicialització dels catàlegs de criatures
        catalegCriatures = new TreeMap<>(); // Catàleg principal de criatures per zona i gènere
        criaturesCapturades = new TreeMap<>(); // Llista de criatures capturades (inicialment buida)
        criaturesEscapades = new TreeMap<>(); // Llista de criatures escapades (inicialment buida)

        Genere aiguard = new Genere("aiguard", 0.01f, Color.MAGENTA, 10, 0);
        Genere focguard = new Genere("focguard", 0.015f, Color.GREEN, 15, 1);
        Genere tornadrac = new Genere("tornadrac", 0.02f, Color.RED, 20, 2.5f);
        Genere vapordrac = new Genere("vapordrac", 0.025f, Color.BLUE, 30, 3.5f);


        Genere[] generes = {aiguard, focguard, tornadrac, vapordrac};
        int id = 0;
        int especie;
        float x,y;
        Random ran = new Random();



        for (Genere genere : generes) {

            for (int i = 0; i < 125; i++) {
                id++;
                especie = ran.nextInt(8)+1;
                x = ran.nextFloat()*bmp.getWidth();
                y = ran.nextFloat()*bmp.getHeight();
                Criatura criatura = new Criatura(id+1, genere, especie, x, y);
                Zona zonaCriatura = findZone((int) x, (int) y);



                // 1. Obtenir el mapa de gèneres per a la zona actual.
                Map<Genere, HashSet<Criatura>> criaturesPerGenereEnZona;
                Log.d("Llegim de zona: ", " x: " + x + " y: " + y);

                // Controlam que si una criatura apareix en una posició sense zona asignada
                // La seva zona rebi el nom "altre"
                String nomZona;
                if(zonaCriatura != null){
                    nomZona = zonaCriatura.getNomOficial();
                } else {
                    nomZona = "altre";
                }
                criatura.setZonaActualCataleg(nomZona);

                if (catalegCriatures.containsKey(nomZona)) {
                    criaturesPerGenereEnZona = catalegCriatures.get(nomZona);
                } else {
                    criaturesPerGenereEnZona = new HashMap<>();
                    catalegCriatures.put(nomZona, criaturesPerGenereEnZona);
                }

                // 2. Obtenir la llista de criatures per al gènere actual dins d'aquest mapa de gèneres.
                HashSet<Criatura> llistaCriaturesDelGenere;

                if (criaturesPerGenereEnZona.containsKey(genere)) {
                    llistaCriaturesDelGenere = criaturesPerGenereEnZona.get(genere);
                } else {
                    llistaCriaturesDelGenere = new HashSet<>();
                    criaturesPerGenereEnZona.put(genere, llistaCriaturesDelGenere);
                }

                // 3. Afegeix la nova criatura a la llista obtinguda/creada.
                llistaCriaturesDelGenere.add(criatura);
            }
        }
    }



    private void combat(Criatura criatura){

        // La criatura no es visible
        if (fe < zoomMaxim - (criatura.getGenere().getDistanciaDeteccio()+5)/5){
            return;
        }

        // Ja hi ha un combat en marcha
        if (currentCombatDialog != null && currentCombatDialog.isShowing()) {
            return;
        }

        Dialog dialog = new Dialog(findViewById(R.id.surfaceView).getContext());
        currentCombatDialog = dialog;
        dialog.setContentView(R.layout.dialog);
        dialog.getWindow().setLayout(SV.getWidth(), SV.getHeight());
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView textResultat = dialog.findViewById(R.id.resultat);
        ImageView imgCriatura = dialog.findViewById(R.id.imageCriatura);
        setCreatureImage(imgCriatura, criatura);

        Button botoPedra = dialog.findViewById(R.id.buttonPedra);
        Button botoPaper = dialog.findViewById(R.id.buttonPaper);
        Button botoTisores = dialog.findViewById(R.id.buttonTisores);

        Button botoResposta = dialog.findViewById(R.id.buttonResposta);
        botoResposta.setVisibility(View.INVISIBLE);

        String[] opcions = {"pedra", "paper", "tisores"};

        View.OnClickListener listener = v -> {
            String eleccioJugador = "";

            if (v.getId() == R.id.buttonPedra) {
                eleccioJugador = opcions[0];
            } else if (v.getId() == R.id.buttonPaper) {
                eleccioJugador = opcions[1];
            } else if (v.getId() == R.id.buttonTisores) {
                eleccioJugador = opcions[2];
            }

            String eleccioCriatura = opcions[(int) (Math.random() * 3)];
            if(eleccioCriatura == "pedra"){
                botoResposta.setForeground(ContextCompat.getDrawable(context,R.drawable.pedra));
            } else if(eleccioCriatura == "paper"){
                botoResposta.setForeground(ContextCompat.getDrawable(context,R.drawable.paper));
            } else if(eleccioCriatura == "tisores"){
                botoResposta.setForeground(ContextCompat.getDrawable(context,R.drawable.tisores));
            }
            botoResposta.setVisibility(View.VISIBLE);

            Context context = getApplicationContext () ;
            int duration = Toast.LENGTH_LONG;

            // 0: Empat, 1: Victòria, 2: Derrota
            final int resultatCombat;

            if (eleccioJugador.equals(eleccioCriatura)) {
                resultatCombat = 0;
            } else if (reglesDeJoc.get(eleccioJugador).equals(eleccioCriatura)) {
                resultatCombat = 1;
                if(eleccioJugador == "pedra"){
                    botoPedra.setForeground(ContextCompat.getDrawable(context,R.drawable.pedrax));
                } else if(eleccioJugador == "paper"){
                    botoPaper.setForeground(ContextCompat.getDrawable(context,R.drawable.paperx));
                } else if(eleccioJugador == "tisores"){
                    botoTisores.setForeground(ContextCompat.getDrawable(context,R.drawable.tisoresx));
                }
                CharSequence text = "Has capturat un " + criatura.getGenere().getName();
                eliminarDelCataleg(criatura);
                Zona zonaCaptura = findZone((int)criatura.getX(), (int)criatura.getY());
                mostrarInventari();
                criaturesCapturades.put(criatura, zonaCaptura); // Guardas criatura + zona
                textResultat.setText(text);
                puntos += criatura.getGenere().getPuntsCompensacio();
                textViewPuntos.setText(puntos + " punts");
                Toast toast = Toast.makeText(context,text,duration);
                toast.show();
            } else {
                resultatCombat = 2;
                CharSequence text = "S'ha escapat un " + criatura.getGenere().getName();
                textResultat.setText(text);
                eliminarDelCataleg(criatura);
                criaturesEscapades.put(criatura,findZone((int)criatura.getX(),(int)criatura.getY()));
                Toast toast = Toast.makeText(context,text,duration);
                toast.show();
            }


            botoPedra.setEnabled(false);
            botoPaper.setEnabled(false);
            botoTisores.setEnabled(false);

            new CountDownTimer(2000, 1000) {
                @Override public void onTick(long millisUntilFinished) {

                }
                @Override public void onFinish() {
                    botoPedra.setEnabled(true);
                    botoPaper.setEnabled(true);
                    botoTisores.setEnabled(true);

                    ferAnimacioEspera(botoPedra);
                    ferAnimacioEspera(botoPaper);
                    ferAnimacioEspera(botoTisores);

                    if(resultatCombat != 0){ // Si no es empat
                        // Tancar el diàleg i netejar la referència
                        if (currentCombatDialog != null) {
                            currentCombatDialog.dismiss();
                            currentCombatDialog = null;
                            dibuixaMapa(); // Redibuixar el mapa per actualitzar la visualització
                        }
                    }
                }
            }.start();
        };

        botoPedra.setOnClickListener(listener);
        botoPaper.setOnClickListener(listener);
        botoTisores.setOnClickListener(listener);

        dialog.show();
    }
    private void setCreatureImage(ImageView imageView, Criatura criatura) {
        String genere = criatura.getGenere().getName().toLowerCase();
        int especie = criatura.getEspecie();
        String imageName = genere + especie;

        int resId = getResources().getIdentifier(imageName, "drawable", getPackageName());

        if (resId != 0) {
            imageView.setImageResource(resId);
        }
    }

    private void ferAnimacioEspera(View boto) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(boto, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(boto, "scaleY", 1f, 1.1f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(500);
        set.setInterpolator(new LinearInterpolator());
        set.start();
    }

    private void dibuixaImatge() {
        if (SV.getHolder().getSurface().isValid()) {
            int alt = SV.getHeight();
            int ampla = SV.getWidth();

            Canvas canvas = SV.getHolder().lockCanvas();
            canvas.drawColor(Color.BLACK); // Fons negre

            // Dibuixem la imatge ocupant tot el SurfaceView
            Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
            Rect dst = new Rect(0, 0, ampla, alt);
            canvas.drawBitmap(bmp, src, dst, new Paint());

            SV.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void dibuixaMapa() {
        if (SV.getHolder().getSurface().isValid()) {
            int amplaPantalla = SV.getWidth();
            int altPantalla = SV.getHeight();

            float w_ = amplaPantalla / fe;
            float h_ = altPantalla / fe;

            float x1 = Cx - w_ / 2f;
            float y1 = Cy - h_ / 2f;
            float x2 = Cx + w_ / 2f;
            float y2 = Cy + h_ / 2f;

            // Assegurem límits perquè no surti fora de la imatge
            x1 = Math.min(Math.max(0, x1), bmp.getWidth() - w_);
            y1 = Math.min(Math.max(0, y1), bmp.getHeight() - h_);
            x2 = Math.max(Math.min(bmp.getWidth(), x2),w_);
            y2 = Math.max(Math.min(bmp.getHeight(), y2),h_);

            Rect src = new Rect((int)x1, (int)y1, (int)x2, (int)y2);
            Rect dst = new Rect(0, 0, amplaPantalla, altPantalla);

            Canvas canvas = SV.getHolder().lockCanvas();
            canvas.drawColor(Color.BLACK);
            canvas.drawBitmap(bmp, src, dst, new Paint());

            boolean hiHaCriatures = false;

            // Dibuixar les criatures
            Paint criaturaPaint = new Paint();
            float criaturaSize = 20; // Mida del quadrat de la criatura en píxels de pantalla

            if (catalegCriatures != null) {
                for (Map.Entry<String, Map<Genere, HashSet<Criatura>>> entryZona : catalegCriatures.entrySet()) {
                    for (Map.Entry<Genere, HashSet<Criatura>> entryGenere : entryZona.getValue().entrySet()) {
                        for (Criatura criatura : entryGenere.getValue()) {
                            // Calcular les coordenades de la criatura a la pantalla
                            // posX i posY són les coordenades absolutes de la criatura al bitmap
                            float posX = criatura.getX();
                            float posY = criatura.getY();

                            // Transformar coordenades del mapa a coordenades de pantalla
                            // (coordenada_mapa - offset_mapa_visible_x) * factor_escala
                            float screenX = (posX - x1) * fe;
                            float screenY = (posY - y1) * fe;

                            // Dibuixar la criatura només si és visible a la pantalla
                            if (screenX >= -criaturaSize && screenX <= amplaPantalla + criaturaSize &&
                                    screenY >= -criaturaSize && screenY <= altPantalla + criaturaSize
                                && fe >= zoomMaxim - (criatura.getGenere().getDistanciaDeteccio()+5)/5) {
                                hiHaCriatures = true;
                                // Obtenir el color de la criatura segons el seu gènere
                                int color = criatura.getGenere().getColorDetector();
                                criaturaPaint.setColor(color);
                                criaturaPaint.setStyle(Paint.Style.FILL);

                                // Dibuixa el quadrat de la criatura
                                canvas.drawRect(screenX - criaturaSize / 2, screenY - criaturaSize / 2,
                                        screenX + criaturaSize / 2, screenY + criaturaSize / 2, criaturaPaint);
                            }
                        }
                    }
                }
            }
            if (hiHaCriatures){
                Paint circlePaint = new Paint();
                circlePaint.setColor(Color.WHITE); // Color del cercle
                circlePaint.setStyle(Paint.Style.STROKE); // Dibuixa només el contorn
                circlePaint.setStrokeWidth(5); // Gruix del contorn del cercle

                // Calcula el centre del SurfaceView
                float centerX = amplaPantalla / 2f;
                float centerY = altPantalla / 2f;

                // Defineix el radi del cercle
                float radius = 30; // Ajusta aquest valor per fer el cercle més gran o més petit

                // Dibuixa el cercle
                canvas.drawCircle(centerX, centerY, radius, circlePaint);
            } else {
                Paint crosshairPaint = new Paint();
                crosshairPaint.setColor(Color.WHITE); // Color of the crosshair
                crosshairPaint.setStrokeWidth(5); // Thickness of the crosshair lines

                // Calculate the center of the SurfaceView
                float centerX = amplaPantalla / 2f;
                float centerY = altPantalla / 2f;

                // Define the length of the crosshair arms
                float armLength = 30; // Adjust this value to make the crosshair larger or smaller

                // Draw horizontal line of the crosshair
                canvas.drawLine(centerX - armLength, centerY, centerX + armLength, centerY, crosshairPaint);

                // Draw vertical line of the crosshair
                canvas.drawLine(centerX, centerY - armLength, centerX, centerY + armLength, crosshairPaint);
            }

            SV.getHolder().unlockCanvasAndPost(canvas);

            textViewZoom.setText(String.format("x %.2f", fe/zoomMaxim));
        }
    }

    private void ferZoomIn() {
        Log.d("ZOOM", "Zoom In");
        if (fe + incrementZoom <= zoomMaxim) {
            fe += incrementZoom;
        } else {
            fe = zoomMaxim;
        }
        dibuixaMapa();
    }

    private void ferZoomOut() {
        Log.d("ZOOM", "Zoom OUT");

        if (fe - incrementZoom >= zoomMinim) {
            fe -= incrementZoom;
        } else {
            fe = zoomMinim;
        }
        dibuixaMapa();
    }

    private void moure(int dx, int dy) {
        Cx += dx / fe;
        Cy += dy / fe;
        Cx = Math.max(0, Math.min(bmp.getWidth(), Cx));
        Cy = Math.max(0, Math.min(bmp.getHeight(), Cy));
        dibuixaMapa();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        if (Escalando) {
            return true;
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(event);
                break;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1) {
                    handleMove(event);
                }
                break;

            case MotionEvent.ACTION_UP:
                handleTouchUp(event);
                actualitzaInformacioZonaActual();
                break;
        }

        return true;
    }

    private void handleTouchDown(MotionEvent event) {
        if (!Escalando) {
            cursorX = event.getX();
            cursorY = event.getY();
            Moviendo = true;
        }
    }

    private void handleMove(MotionEvent event) {
        if (currentCombatDialog != null && currentCombatDialog.isShowing()) {
            return;
        }
        if (Moviendo && !Escalando) {
            float x = event.getX();
            float y = event.getY();

            float dx = (x - cursorX) / fe;
            float dy = (y - cursorY) / fe;

            Cx -= dx;
            Cy -= dy;

            Cx = Math.max(0, Math.min(bmp.getWidth(), Cx));
            Cy = Math.max(0, Math.min(bmp.getHeight(), Cy));

            criaturesEscapen(Cx, Cy);
            Criatura criatura = cercarCriaturesProperes(Cx, Cy);
            if(criatura != null){
                combat(criatura);
            }

            cursorX = x;
            cursorY = y;

            dibuixaMapa();
        }
    }

    private void eliminarDelCataleg(Criatura criatura) {
        if (catalegCriatures == null || criatura == null) {
            return;
        }

        Zona zonaOfCreature = findZone((int)criatura.getX(), (int)criatura.getY());
        String nomZona = (zonaOfCreature != null) ? zonaOfCreature.getNomOficial() : "altre";

        Genere nomGenere = criatura.getGenere();

        Map<Genere, HashSet<Criatura>> criaturesPerGenereEnZona = catalegCriatures.get(nomZona);
        if (criaturesPerGenereEnZona != null) {
            HashSet<Criatura> llistaCriaturesDelGenere = criaturesPerGenereEnZona.get(nomGenere);
            if (llistaCriaturesDelGenere != null) {
                boolean removed = llistaCriaturesDelGenere.remove(criatura);
                if (removed) {
                    Log.d("removeCreature", "Criatura " + criatura.getNom() + " eliminada del catàleg de mapa.");
                    if (llistaCriaturesDelGenere.isEmpty()) {
                        criaturesPerGenereEnZona.remove(nomGenere);
                        if (criaturesPerGenereEnZona.isEmpty()) {
                            catalegCriatures.remove(nomZona);
                        }
                    }
                }
            }
        }
    }

    // Substitueix el teu mètode criaturesEscapen existent per aquest:
    private void criaturesEscapen(float mapX, float mapY){
        List<MovementInfo> movementsToPerform = new ArrayList<>();

        // Fase 1: Recollida. Obtenim totes les criatures en una llista temporal plana.
        List<Criatura> allCriaturesCurrentlyInCatalog = new ArrayList<>();
        for (Map.Entry<String, Map<Genere, HashSet<Criatura>>> entryZona : catalegCriatures.entrySet()) {
            Map<Genere, HashSet<Criatura>> criaturesPerGenereEnZona = entryZona.getValue();
            if (criaturesPerGenereEnZona != null) {
                for (Map.Entry<Genere, HashSet<Criatura>> entryGenere : criaturesPerGenereEnZona.entrySet()) {
                    allCriaturesCurrentlyInCatalog.addAll(new ArrayList<>(entryGenere.getValue()));
                }
            }
        }

        for (Criatura criatura : allCriaturesCurrentlyInCatalog) {
            if (criaturesEscapades.containsKey(criatura) || criaturesCapturades.containsKey(criatura)) {
                continue;
            }

            float dx = criatura.getX() - mapX;
            float dy = criatura.getY() - mapY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance <= DISTANCIA_ESCAPE) {
                float newX = criatura.getX() + dx * criatura.getGenere().getVelocitat() * 10;
                float newY = criatura.getY() + dy * criatura.getGenere().getVelocitat() * 10;

                movementsToPerform.add(new MovementInfo(criatura, newX, newY));
            }
        }

        // Fase 2: Aplicació. Apliquem els moviments.
        for (MovementInfo movement : movementsToPerform) {
            updateCreaturePositionAndCatalog(movement.getCriatura(), movement.getNewX(), movement.getNewY());
        }

        dibuixaMapa();
    }

    public void updateCreaturePositionAndCatalog(Criatura criatura, float newX, float newY) {
        if (criatura == null) {
            return;
        }

        String oldZoneName = criatura.getZonaActualCataleg();

        criatura.setX(newX);
        criatura.setY(newY);

        Zona newZoneObject = findZone((int)newX, (int)newY);
        String newZoneName = (newZoneObject != null) ? newZoneObject.getNomOficial() : "altre";


        // Si les coordenades estan fora dels límits del mapa, la criatura s'escapa definitivament
        if (newX < 0 || newX > bmp.getWidth() || newY < 0 || newY > bmp.getHeight()) {
            if (!criaturesEscapades.containsKey(criatura)) {
                if (oldZoneName != null) {
                    Map<Genere, HashSet<Criatura>> criaturesPerGenereEnOldZone = catalegCriatures.get(oldZoneName);
                    if (criaturesPerGenereEnOldZone != null) {
                        HashSet<Criatura> llistaCriaturesDelGenere = criaturesPerGenereEnOldZone.get(criatura.getGenere());
                        if (llistaCriaturesDelGenere != null) {
                            llistaCriaturesDelGenere.remove(criatura);
                            if (llistaCriaturesDelGenere.isEmpty()) {
                                criaturesPerGenereEnOldZone.remove(criatura.getGenere());
                                if (criaturesPerGenereEnOldZone.isEmpty()) {
                                    catalegCriatures.remove(oldZoneName);
                                }
                            }
                        }
                    }
                }
                criaturesEscapades.put(criatura, new Zona("altre", "altre", -1, -1, -1, -1));
                criatura.setZonaActualCataleg("escapada");
                Log.d("CatalogUpdate", "Criatura " + criatura.getNom() + " (ID: " + criatura.getId() + ") s'ha escapat del mapa.");
            }
            return;
        }

        if (oldZoneName == null || !oldZoneName.equals(newZoneName)) {
            if (oldZoneName != null) {
                Map<Genere, HashSet<Criatura>> criaturesPerGenereEnOldZone = catalegCriatures.get(oldZoneName);
                if (criaturesPerGenereEnOldZone != null) {
                    HashSet<Criatura> llistaCriaturesDelGenere = criaturesPerGenereEnOldZone.get(criatura.getGenere());
                    if (llistaCriaturesDelGenere != null) {
                        boolean removed = llistaCriaturesDelGenere.remove(criatura);
                        if (removed) {
                            if (llistaCriaturesDelGenere.isEmpty()) {
                                criaturesPerGenereEnOldZone.remove(criatura.getGenere());
                                if (criaturesPerGenereEnOldZone.isEmpty()) {
                                    catalegCriatures.remove(oldZoneName);
                                }
                            }
                            Log.d("CatalogUpdate", "Criatura " + criatura.getNom() + " (ID: " + criatura.getId() + ") eliminada de la zona antiga '" + oldZoneName + "'.");
                        } else {
                            Log.w("CatalogUpdate", "No s'ha pogut eliminar la criatura " + criatura.getNom() + " (ID: " + criatura.getId() + ") de la zona antiga '" + oldZoneName + "'. Potser ja no hi era o la referència ha canviat?");
                        }
                    }
                }
            }

            criatura.setZonaActualCataleg(newZoneName);
            catalegCriatures
                    .computeIfAbsent(newZoneName, k -> new HashMap<>())
                    .computeIfAbsent(criatura.getGenere(), k -> new HashSet<>())
                    .add(criatura);

            Log.d("CatalogUpdate", "Criatura " + criatura.getNom() + " (ID: " + criatura.getId() + ") moguda i actualitzada al catàleg de la zona: " + newZoneName);

        } else {
            Log.d("CatalogUpdate", "Criatura " + criatura.getNom() + " (ID: " + criatura.getId() + ") es va moure dins de la mateixa zona: " + newZoneName);
        }
    }

    private static class MovementInfo {
        private Criatura criatura;
        private float newX;
        private float newY;

        public MovementInfo(Criatura criatura, float newX, float newY) {
            this.criatura = criatura;
            this.newX = newX;
            this.newY = newY;
        }

        public Criatura getCriatura() {
            return criatura;
        }

        public float getNewX() {
            return newX;
        }

        public float getNewY() {
            return newY;
        }
    }


    private Criatura cercarCriaturesProperes(float mapX, float mapY){
        // Iterar sobre totes les zones i tots els gèneres per trobar qualsevol criatura
        for (Map.Entry<String, Map<Genere, HashSet<Criatura>>> entryZona : catalegCriatures.entrySet()) {
            Map<Genere, HashSet<Criatura>> criaturesPerGenereEnZona = entryZona.getValue();
            if (criaturesPerGenereEnZona != null) {
                for (Map.Entry<Genere, HashSet<Criatura>> entryGenere : criaturesPerGenereEnZona.entrySet()) {
                    Iterator<Criatura> it = entryGenere.getValue().iterator();
                    while (it.hasNext()) {
                        Criatura criatura = it.next();
                        float dx = criatura.getX() - mapX;
                        float dy = criatura.getY() - mapY;
                        float distance = (float) Math.sqrt(dx * dx + dy * dy);

                        if (distance <= DISTANCIA_COMBAT) {
                            return criatura; // Criatura trobada
                        }
                    }
                }
            }
        }
        return null;
    }

    private void handleTouchUp(MotionEvent event) {
        Moviendo = false;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Escalando = true;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newFe = fe * scaleFactor;
            newFe = Math.max(zoomMinim, Math.min(newFe, zoomMaxim));

            if (newFe != fe) {
                fe = newFe;
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                adjustCenterForZoom(focusX, focusY, scaleFactor);

                dibuixaMapa();
                actualitzaInformacioZonaActual();
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Escalando = false;
        }
    }

    private void adjustCenterForZoom(float focusX, float focusY, float scaleFactor) {
        float mapFocusX = Cx + (focusX - SV.getWidth()/2f) / fe;
        float mapFocusY = Cy + (focusY - SV.getHeight()/2f) / fe;

        Cx = mapFocusX - (mapFocusX - Cx) / scaleFactor;
        Cy = mapFocusY - (mapFocusY - Cy) / scaleFactor;

        Cx = Math.max(0, Math.min(bmp.getWidth(), Cx));
        Cy = Math.max(0, Math.min(bmp.getHeight(), Cy));
    }

    public String llegirJSON ( Context context , int id ) {
        String json = null;
        try {
            InputStream is = context.getResources().openRawResource(id);
            int size = is.available();
            byte [] buffer = new byte [size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public String miraZona(int x, int y){
        Zona zona = findZone(x, y);
        return zona.getNomPopular();
    }

    public void crearEstructuresDeDadesDeZones (String contingutJSON){
        try {
            JSONObject arrel = new JSONObject(contingutJSON);
            JSONArray arrayZones = arrel.getJSONArray("zones_coords");

            for (int i = 0; i < arrayZones.length(); i++) {
                JSONObject zonaObj = arrayZones.getJSONObject(i);

                String nomPopular = zonaObj.getString("zona");
                String nomOficial = zonaObj.getString("nom");
                int x1 = zonaObj.getInt("x1");
                int y1 = zonaObj.getInt("y1");
                int x2 = zonaObj.getInt("x2");
                int y2 = zonaObj.getInt("y2");

                Zona z = new Zona(nomPopular, nomOficial, x1, y1, x2, y2);

                catalegZonesTrie.insert(nomPopular, z);

                catalegZonesMapa.put(nomPopular, z);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actualitzaInformacioZonaActual() {
        if (conjuntoVisible.equals("mapa")) {
            int mapCenterX = (int) Cx;
            int mapCenterY = (int) Cy;

            Zona zonaActual = findZone(mapCenterX, mapCenterY);
            if (zonaActual != null) {
                textViewNomZona.setText(" " + zonaActual.getNomOficial());
            } else {
            }
        }
    }

    private Zona findZone(int mapX, int mapY) {
        for (Zona zona : catalegZonesMapa.values()) {
            if (zona.contains(mapX, mapY)) {
                return zona;
            }
        }
        return null;
    }


    private void iniciarTemporitzadorActualitzacioZona() {
        if (zoneUpdateTimer != null) {
            zoneUpdateTimer.cancel();
        }
        zoneUpdateTimer = new CountDownTimer(Long.MAX_VALUE, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
                actualitzaInformacioZonaActual();
            }

            @Override
            public void onFinish() {
            }
        }.start();
    }

    //funciona bien hay que arreglar el formato
    private void mostrarCriaturesPerZona() {
        runOnUiThread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                if (catalegCriatures == null || catalegCriatures.isEmpty()) {
                    sb.append("<i>No hi ha dades de criatures disponibles</i>");
                    textCriatures.setText(Html.fromHtml(sb.toString()));
                    return;
                }

                // 1. Ordenar zonas alfabéticamente
                TreeMap<String, Map<Genere, HashSet<Criatura>>> zonesOrdenades = new TreeMap<>(catalegCriatures);


                for (Map.Entry<String, Map<Genere, HashSet<Criatura>>> entryZona : zonesOrdenades.entrySet()) {
                    String nomZona = entryZona.getKey();
                    Map<Genere, HashSet<Criatura>> criaturesPerGenere = entryZona.getValue();

                    sb.append("<p style='margin-bottom:5px;'><b><font color='#333333'>")
                            .append("A la zona ")
                            .append(nomZona)
                            .append(" hi ha:")
                            .append("</font></b></p>");

                    // 2. Ordenar géneros alfabéticamente
                    TreeMap<String, Genere> generesOrdenats = new TreeMap<>();
                    for (Genere g : criaturesPerGenere.keySet()) {
                        generesOrdenats.put(g.getName(), g);
                    }

                    // Detalle por género
                    if (!criaturesPerGenere.isEmpty()) {
                        sb.append("<div style='margin-left:15px;'>");
                        for (Map.Entry<String, Genere> entryGenere : generesOrdenats.entrySet()) {
                            Genere genere = entryGenere.getValue();
                            int quantitat = criaturesPerGenere.get(genere).size();

                            // Valor
                            sb.append(" <font color='")
                                    .append(String.format("#%06X", (0xFFFFFF & Color.BLACK)))
                                    .append("'>")
                                    .append("\t\t\t")
                                    .append(quantitat)
                                    .append("</font>");

                            // Genere
                            sb.append(" <font color='")
                                    .append(String.format("#%06X", (0xFFFFFF & genere.getColorDetector())))
                                    .append("'>").append(genere.getName()).append("</font>")
                                    .append("<br>");
                        }
                        sb.append("</div>");
                    }
                }


                textCriatures.setText(Html.fromHtml(sb.toString()));
                textCriatures.setMovementMethod(new ScrollingMovementMethod());

            } catch (Exception e) {
                Log.e("ERROR", "Excepción en mostrarCriaturesPerZona: " + e.getMessage(), e);
                textCriatures.setText("Error al mostrar les dades. Revisa Logcat.");
            }
        });
    }

    private void mostrarZonesDelMapa() {
        runOnUiThread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("<p style='font-size:18px; font-weight:bold; color:#000000;'> <b> ZONES DEL MAPA </b> </p>");

                if (catalegZonesMapa == null || catalegZonesMapa.isEmpty()) {
                    sb.append("<i>No hi ha zones definides al mapa</i>");
                    textCriatures.setText(Html.fromHtml(sb.toString()));
                    return;
                }

                // 1. Ordenar zonas alfabéticamente
                TreeMap<String, Zona> zonesOrdenades = new TreeMap<>(catalegZonesMapa);

                for (Map.Entry<String, Zona> entry : zonesOrdenades.entrySet()) {
                    Zona zona = entry.getValue();

                    // Cabecera de zona con dimensiones
                    sb.append("<p style='margin-bottom:0px;'><font color='#999999'>")
                            .append("\t\t\t")
                            .append(zona.getNomPopular())
                            .append(": (").append(zona.getX1()).append(":").append(zona.getX2())
                            .append(") (").append(zona.getY1()).append(":").append(zona.getY2())
                            .append(")</p>");

                }


                textCriatures.setText(Html.fromHtml(sb.toString()));
                textCriatures.setMovementMethod(new ScrollingMovementMethod());

            } catch (Exception e) {
                Log.e("ERROR", "Excepció en mostrarZonesDelMapa: " + e.getMessage(), e);
                textCriatures.setText("Error al mostrar les zones. Revisa Logcat.");
            }
        });
    }

    //funciona bien hay que arreglar el formato
    private void mostrarCriaturesCapturades() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Criatures Capturades</h2><br>");

        if (criaturesCapturades.isEmpty()) {
            sb.append("Encara no has capturat cap criatura.<br>");
        } else {
            // Usamos TreeMap para ordenar alfabéticamente por nombre de criatura
            TreeMap<String, Map.Entry<Criatura, Zona>> criaturesOrdenades = new TreeMap<>();

            for (Map.Entry<Criatura, Zona> entry : criaturesCapturades.entrySet()) {
                criaturesOrdenades.put(entry.getKey().getNom(), entry);
            }

            for (Map.Entry<String, Map.Entry<Criatura, Zona>> entry : criaturesOrdenades.entrySet()) {
                Criatura c = entry.getValue().getKey();
                Zona z = entry.getValue().getValue();

                sb.append("<font color='")
                        .append(String.format("#%06X", (0xFFFFFF & c.getGenere().getColorDetector())))
                        .append("'><b>")
                        .append(c.getGenere().getName())  // Ej: "tornadrac"
                        .append(" ")
                        .append(c.getEspecie())          // Ej: "3"
                        //.append("</b></font><br>")
                       // .append("&nbsp;&nbsp;• Zona: <i>")
                        .append( z.getNomOficial())
                        .append("</i><br>")
                        .append("&nbsp;&nbsp;• Coord: [")
                        .append((int)c.getX())
                        .append(", ")
                        .append((int)c.getY())
                        .append("]<br>");
            }
        }

        // Asegúrate de que el TextView tiene el movimiento habilitado
        textCriatures.setMovementMethod(new ScrollingMovementMethod());
        textCriatures.setText(Html.fromHtml(sb.toString()));
        textCriatures.scrollTo(0, 0);
    }
    private void mostrarCriaturesEscapades() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2 style='color:#FF5722;'>Criatures Escapades</h2><br>");

        if (criaturesEscapades.isEmpty()) {
            sb.append("<i>No s'ha escapat cap criatura.</i><br>");
        } else {
            // Usamos TreeMap para ordenar igual que en capturadas
            TreeMap<String, Map.Entry<Criatura, Zona>> criaturesOrdenades = new TreeMap<>();

            for (Map.Entry<Criatura, Zona> entry : criaturesEscapades.entrySet()) {
                criaturesOrdenades.put(entry.getKey().getNom(), entry);
            }

            for (Map.Entry<String, Map.Entry<Criatura, Zona>> entry : criaturesOrdenades.entrySet()) {
                Criatura c = entry.getValue().getKey();
                Zona z = entry.getValue().getValue();

                sb.append("<p style='margin-bottom:15px;'>")
                        .append("<b><font color='")
                        .append(String.format("#%06X", (0xFFFFFF & c.getGenere().getColorDetector())))
                        .append("'>")
                        .append(c.getGenere().getName())  // Ej: "tornadrac"
                        .append(" ")
                        .append(c.getEspecie())          // Ej: "3"
                       // .append("</font></b><br>")
                       // .append("&nbsp;&nbsp;• Zona: <i>")
                        .append(z.getNomOficial())
                        .append("</i><br>")
                        .append("&nbsp;&nbsp;• Coord: [")
                        .append((int)c.getX())
                        .append(", ")
                        .append((int)c.getY())
                        .append("]<br>")
                        .append("&nbsp;&nbsp;• Estat: <font color='#FF0000'>ESCAPADA</font></p>");
            }
        }

        textCriatures.setMovementMethod(new ScrollingMovementMethod());
        textCriatures.setText(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY));
        textCriatures.scrollTo(0, 0);
    }
}