package com.example.monmistic;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;


public class MainActivity extends AppCompatActivity {

    //////INICIALIZACIONES
    public Button BMapa, BCriatura, BInventario, Bmenos1, Bmenos2, Bmas1, Bmas2;
    //private TextView PZoom, NomZona, Puntos, textCriaturas;
    //private EditText CercaZona;
    private SurfaceView SV, SV2;
    UnsortedLinkedListSet<View> mapaViews;
    UnsortedLinkedListSet<View> craituresViews;
    UnsortedLinkedListSet<View> inventariViews;
    private String conjuntoVisible = ""; // "mapa", "criaturas", "inventario", ""


    private Context context;
    private Bitmap bmp;// imagen del mapa
    private float Cx, Cy;        // Coordenades centrals (en píxels dins del mapa)
    private float fe;            // Factor d'escalat actual (zoom)
    private float zoomMinim;     // Zoom mínim (veure tot el mapa)
    private float zoomMaxim;     // Zoom màxim (detall)
    private float incrementZoom; // Pas de zoom

    ////seguimiento de los dedos
    private float cursorX,cursorY;
    private boolean Moviendo = false;
    private float DistanciaInicial= -1; //Gesto zoom
    private float ZoomInicial= 1f; //zoom inicial gesto pellizco

    private ScaleGestureDetector scaleDetector;
    private boolean Escalando = false;

            // La imatge del mapa
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        inicializarConjuntoMapa();
        inicializarConjuntoCriaturas();
        inicializarConjuntoInventario();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //Mapa View
        BMapa = findViewById(R.id.botonMapa);

        //Criaturas View
        BCriatura = findViewById(R.id.botonCriatura);

        //Inventario View
        BInventario = findViewById(R.id.botonInventario);

        //Crear la imagen del mapa en el Surface View
        context = getApplicationContext();
        SV = findViewById(R.id.surfaceView);

        // Carreguem la imatge només una vegada
        bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.mapam);

        // Esperem que la vista estigui creada
        SV.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                // Ahora sí se puede acceder a tamaño real del SurfaceView
                zoomMinim = (float) SV.getHeight() / bmp.getHeight();  // Veure tota la imatge
                zoomMaxim = zoomMinim * 10;  // Zoom detallat
                incrementZoom = zoomMinim / 4; // Zoom progressiu

                // Centrat inicial (centre del mapa)
                Cx = bmp.getWidth() / 2f;
                Cy = bmp.getHeight() / 2f;

                // Escalat inicial
                fe = zoomMinim;

                dibuixaMapa();  // Mostrar mapa amb zoom inicial
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {}
        });


        BMapa.setOnClickListener(v -> {
            visibilizar("mapa");
        });

        BCriatura.setOnClickListener(v -> {
            visibilizar("criatures");
        });

        BInventario.setOnClickListener(v -> {
            visibilizar("inventari");
        });



        //btnDreta.setOnClickListener(v -> moure(50, 0));
        //btnEsquerra.setOnClickListener(v -> moure(-50, 0));
        //btnAmunt.setOnClickListener(v -> moure(0, -50));
        //btnAvall.setOnClickListener(v -> moure(0, 50));

        //Inicializador de detector de gestos
        scaleDetector = new ScaleGestureDetector(this,new ScaleListener());

    }
    public void inicializarConjuntoMapa(){
        SurfaceView sv = findViewById(R.id.surfaceView);
        TextView tv1 = findViewById(R.id.NomZONA);
        TextView tv2 = findViewById(R.id.PorcentajeZoom);
        TextView tv3 = findViewById(R.id.Puntos);
        TextView tv4 = findViewById(R.id.CercaZona);

        Bmas1 = findViewById(R.id.bottonmas1);
        Bmenos1 = findViewById(R.id.bottonmenos1);
        Bmas2 = findViewById(R.id.bottonmas2);
        Bmenos2 = findViewById(R.id.bottonmenos2);


        mapaViews = new UnsortedLinkedListSet<View>();
        mapaViews.add(sv);
        mapaViews.add(tv1);
        mapaViews.add(tv2);
        mapaViews.add(tv3);
        mapaViews.add(tv4);
        mapaViews.add(Bmas1);
        mapaViews.add(Bmenos1);
        mapaViews.add(Bmas2);
        mapaViews.add(Bmenos2);

        Bmenos2.setOnClickListener(v -> ferZoomOut());
        Bmas2.setOnClickListener(v -> ferZoomIn());
        Bmenos1.setOnClickListener(v -> {
            fe = zoomMinim;
            dibuixaMapa();
        });

        Bmas1.setOnClickListener(v -> {
            fe = zoomMaxim;
            dibuixaMapa();
        });
    }


    public void inicializarConjuntoCriaturas(){
        // --- Asignar nombre a elementos ---
        SurfaceView sv2 = findViewById(R.id.surfaceView2);
        TextView tv = findViewById(R.id.textCriaturas);
        RadioButton rb1 = findViewById(R.id.radioButton);
        RadioButton rb2 = findViewById(R.id.radioButton2);
        RadioButton rb3 = findViewById(R.id.radioButton3);
        RadioButton rb4 = findViewById(R.id.radioButton4);

        // --- Añadir elementos al conjunto ---
        craituresViews = new UnsortedLinkedListSet<View>();
        craituresViews.add(sv2);
        craituresViews.add(tv);
        craituresViews.add(rb1);
        craituresViews.add(rb2);
        craituresViews.add(rb3);
        craituresViews.add(rb4);
    }

    public void inicializarConjuntoInventario(){
        // --- Asignar nombre a elementos ---
        SurfaceView sv = findViewById(R.id.surfaceView);
        Button bt1 = findViewById(R.id.bottonmenos2);
        Button bt2 = findViewById(R.id.bottonmas2);
        Button bt3 = findViewById(R.id.bottonmenos1);
        Button bt4 = findViewById(R.id.bottonmas1);

        // --- Añadir elementos al conjunto ---
        inventariViews = new UnsortedLinkedListSet<View>();
        inventariViews.add(sv);
        inventariViews.add(bt1);
        inventariViews.add(bt2);
        inventariViews.add(bt3);
        inventariViews.add(bt4);
    }

    private void visibilizar(String conjunto){
        for (View view : mapaViews     ) view.setVisibility(View.INVISIBLE);
        for (View view : craituresViews) view.setVisibility(View.INVISIBLE);
        for (View view : inventariViews) view.setVisibility(View.INVISIBLE);

        // Si el conjunto estaba visible, deja todos ocultos
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
           x1 = Math.max(0, x1);
           y1 = Math.max(0, y1);
           x2 = Math.min(bmp.getWidth(), x2);
           y2 = Math.min(bmp.getHeight(), y2);

           Rect src = new Rect((int)x1, (int)y1, (int)x2, (int)y2);
           Rect dst = new Rect(0, 0, amplaPantalla, altPantalla);

           Canvas canvas = SV.getHolder().lockCanvas();
           canvas.drawColor(Color.BLACK);
           canvas.drawBitmap(bmp, src, dst, new Paint());
           SV.getHolder().unlockCanvasAndPost(canvas);
       }
    }

       private void ferZoomIn() {
        Log.d("ZOOM", "Zoom In");
        if (fe + incrementZoom <= zoomMaxim) {
            fe += incrementZoom;
            dibuixaMapa();
        }
    }

       private void ferZoomOut() {
           Log.d("ZOOM", "Zoom OUT");

           if (fe - incrementZoom >= zoomMinim) {
               fe -= incrementZoom;
               dibuixaMapa();
           }
       }

       private void moure(int dx, int dy) {
           Cx += dx / fe;  // Ajustat pel nivell de zoom
           Cy += dy / fe;
           // Assegurar que no surtim del mapa
           Cx = Math.max(0, Math.min(bmp.getWidth(), Cx));
           Cy = Math.max(0, Math.min(bmp.getHeight(), Cy));
           dibuixaMapa();
       }

    /////////P2 USO DE LOS DEDOS

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Primero procesamos el gesto de zoom
        scaleDetector.onTouchEvent(event);

        // Si zoom nada
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

            // Limitar al tamaño del mapa
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

            // Si el zoom ha cambiado lo cambiamos
            if (newFe != fe) {
                fe = newFe;

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                adjustCenterForZoom(focusX, focusY, scaleFactor);

                dibuixaMapa();
            }

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            Escalando = false;
        }
    }
    private void adjustCenterForZoom(float focusX, float focusY, float scaleFactor) {
        // Convertir coordenadas de pantalla a coordenadas del mapa
        float mapFocusX = Cx + (focusX - SV.getWidth()/2f) / fe;
        float mapFocusY = Cy + (focusY - SV.getHeight()/2f) / fe;

        // Ajustar el centro
        Cx = mapFocusX - (mapFocusX - Cx) / scaleFactor;
        Cy = mapFocusY - (mapFocusY - Cy) / scaleFactor;

        //no salir limites
        Cx = Math.max(0, Math.min(bmp.getWidth(), Cx));
        Cy = Math.max(0, Math.min(bmp.getHeight(), Cy));
    }



   }





