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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
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
    private boolean botonesMapaVisibles, botonCiaturasVisibles, botonInventarioVisible = false;
    public Button BMapa, BCriatura, BInventario, Bmenos1, Bmenos2, Bmas1, Bmas2;
    private TextView PZoom, NomZona, Puntos, textCriaturas;
    private EditText CercaZona;
    private SurfaceView SV, SV2;
    Map<String, View> mapaViews = new HashMap<>();
    Map<String, View> craituresViews = new HashMap<>();

    Map<String, View> inventariViews = new HashMap<>();

    private Context context;
    private Bitmap bmp;// imagen del mapa
    private float Cx, Cy;        // Coordenades centrals (en píxels dins del mapa)
    private float fe;            // Factor d'escalat actual (zoom)
    private float zoomMinim;     // Zoom mínim (veure tot el mapa)
    private float zoomMaxim;     // Zoom màxim (detall)
    private float incrementZoom; // Pas de zoom
            // La imatge del mapa
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        //Mapa View
        BMapa = findViewById(R.id.botonMapa);
        mapaViews.put("Bmenos1", findViewById(R.id.bottonmenos1));
        mapaViews.put("Bmenos2", findViewById(R.id.bottonmenos2));
        mapaViews.put("Bmas1", findViewById(R.id.bottonmas1));
        mapaViews.put("Bmas2", findViewById(R.id.bottonmas2));
        mapaViews.put("PZoom", findViewById(R.id.PorcentajeZoom));
        mapaViews.put("NomZona", findViewById(R.id.NomZONA));
        mapaViews.put("Puntos", findViewById(R.id.Puntos));
        mapaViews.put("CercaZona", findViewById(R.id.CercaZona));
        mapaViews.put("SV", findViewById(R.id.surfaceView));

        //Criaturas View
        BCriatura = findViewById(R.id.botonCriatura);
        craituresViews.put("SV2", findViewById(R.id.surfaceView2));
        craituresViews.put("textCriaturas", findViewById(R.id.textCriaturas));
        craituresViews.put("radioButton", findViewById(R.id.radioButton));
        craituresViews.put("radioButton2", findViewById(R.id.radioButton2));
        craituresViews.put("radioButton3", findViewById(R.id.radioButton3));
        craituresViews.put("radioButton4", findViewById(R.id.radioButton4));

        //Inventario View
        BInventario = findViewById(R.id.botonInventario);
        inventariViews.put("SV", findViewById(R.id.surfaceView));
        //FindVIews de Botones
        Bmenos2 = findViewById(R.id.bottonmenos2);
        Bmas2 = findViewById(R.id.bottonmas2);
        Bmenos1= findViewById(R.id.bottonmenos1);
        Bmas1 = findViewById(R.id.bottonmas1);


        //Funcion hacer visible Mapa
        BMapa.setOnClickListener(v -> {
            for (Map.Entry<String, View> entrada : mapaViews.entrySet()) {
                View view = entrada.getValue();
                if (botonesMapaVisibles) {
                    view.setVisibility(View.GONE);
                } else {
                    view.setVisibility(View.VISIBLE);

                }
            }
            dibuixaMapa();
            botonesMapaVisibles = !botonesMapaVisibles;
        });
        //Funcion hacer visible Criaturas
        BCriatura.setOnClickListener(v -> {
            for (Map.Entry<String, View> entrada : craituresViews.entrySet()) {
                View view = entrada.getValue();
                if (botonCiaturasVisibles) {
                    view.setVisibility(View.GONE);
                } else {
                    view.setVisibility(View.VISIBLE);

                }
            }
            botonCiaturasVisibles = !botonCiaturasVisibles;
        });

        BInventario.setOnClickListener(v -> {
            for (Map.Entry<String, View> entrada : inventariViews.entrySet()) {
                View view = entrada.getValue();
                if (botonInventarioVisible) {
                    view.setVisibility(View.GONE);
                } else {
                    view.setVisibility(View.VISIBLE);

                }
            }
            botonInventarioVisible = !botonInventarioVisible;
        });

        //Crear la imagen del mapa en el Surface View
        context = getApplicationContext();
        SV = findViewById(R.id.surfaceView);

        // Carreguem la imatge només una vegada
        bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.mapam); // substitueix "teva_imatge" pel nom real

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





        //BMapa.setOnClickListener (v -> dibuixaMapa());
        Bmenos2.setOnClickListener(v -> ferZoomOut());
        Bmas2.setOnClickListener(v -> ferZoomIn());
        Bmenos1.setOnClickListener(v -> {
            fe = zoomMaxim;
            dibuixaMapa();
        });

        Bmas1.setOnClickListener(v -> {
            fe = zoomMinim;
            dibuixaMapa();
        });

        //btnDreta.setOnClickListener(v -> moure(50, 0));
        //btnEsquerra.setOnClickListener(v -> moure(-50, 0));
        //btnAmunt.setOnClickListener(v -> moure(0, -50));
        //btnAvall.setOnClickListener(v -> moure(0, 50));

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





   }










