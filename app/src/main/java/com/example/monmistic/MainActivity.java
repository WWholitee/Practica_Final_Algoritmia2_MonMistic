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
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //////INICIALIZACIONES
    public Button BMapa, BCriatura, BInventario, Bmenos1, Bmenos2, Bmas1, Bmas2;
    private TextView textViewNomZona;
    private TextView textViewPuntos;
    private EditText editTextCercaZona;
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
            combat();
            visibilizar("inventari");
            if (zoneUpdateTimer != null) {
                zoneUpdateTimer.cancel();
            }
        });

        scaleDetector = new ScaleGestureDetector(this,new ScaleListener());
    }

    public void inicializarConjuntoMapa(){
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

    public void inicializarConjuntoCriaturas(){
        SurfaceView sv2 = findViewById(R.id.surfaceView2);
        RadioButton rb1 = findViewById(R.id.radioButton);
        RadioButton rb2 = findViewById(R.id.radioButton2);
        RadioButton rb3 = findViewById(R.id.radioButton3);
        RadioButton rb4 = findViewById(R.id.radioButton4);

        craituresViews = new UnsortedLinkedListSet<View>();
        craituresViews.add(sv2);
        craituresViews.add(rb1);
        craituresViews.add(rb2);
        craituresViews.add(rb3);
        craituresViews.add(rb4);
    }

    public void inicializarConjuntoInventario(){
        SurfaceView sv = findViewById(R.id.surfaceView);

        inventariViews = new UnsortedLinkedListSet<View>();
        inventariViews.add(sv);
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

    private void combat(){
        Dialog dialog = new Dialog(findViewById(R.id.surfaceView).getContext());
        dialog.setContentView(R.layout.dialog);
        dialog.getWindow().setLayout(SV.getWidth(), SV.getHeight());
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView textResultat = dialog.findViewById(R.id.resultat);
        ImageView imgCriatura = dialog.findViewById(R.id.imageCriatura);

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

            if (eleccioJugador.equals(eleccioCriatura)) {
            } else if (reglesDeJoc.get(eleccioJugador).equals(eleccioCriatura)) {
                if(eleccioJugador == "pedra"){
                    botoPedra.setForeground(ContextCompat.getDrawable(context,R.drawable.pedrax));
                } else if(eleccioJugador == "paper"){
                    botoPaper.setForeground(ContextCompat.getDrawable(context,R.drawable.paperx));
                } else if(eleccioJugador == "tisores"){
                    botoTisores.setForeground(ContextCompat.getDrawable(context,R.drawable.tisoresx));
                }
                CharSequence text = "Has capturat un Pokemon";
                textResultat.setText(text);
                Toast toast = Toast.makeText(context,text,duration);
                toast.show();
            } else {
                CharSequence text = "Has capturat un Digimon";
                textResultat.setText(text);
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
                }
            }.start();

        };

        botoPedra.setOnClickListener(listener);
        botoPaper.setOnClickListener(listener);
        botoTisores.setOnClickListener(listener);

        dialog.show();
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
        if (Moviendo && !Escalando) {
            float x = event.getX();
            float y = event.getY();

            float dx = (x - cursorX) / fe;
            float dy = (y - cursorY) / fe;

            Cx -= dx;
            Cy -= dy;

            Cx = Math.max(0, Math.min(bmp.getWidth(), Cx));
            Cy = Math.max(0, Math.min(bmp.getHeight(), Cy));

            cursorX = x;
            cursorY = y;

            dibuixaMapa();
        }
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

                Zona z = new Zona(nomOficial, x1, y1, x2, y2);

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

            Zona zonaActual = findZoneAtCurrentMapCenter(mapCenterX, mapCenterY);
            if (zonaActual != null) {
                textViewNomZona.setText("Zona: " + zonaActual.getNomOficial());
            } else {
                textViewNomZona.setText("Zona: Desconeguda");
            }
        }
    }

    private Zona findZoneAtCurrentMapCenter(int mapX, int mapY) {
        for (Zona zona : catalegZonesMapa.values()) {
            // Replaced zona.getBounds().contains(mapX, mapY) with the new contains method
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
}