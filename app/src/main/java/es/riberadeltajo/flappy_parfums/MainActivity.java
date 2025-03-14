package es.riberadeltajo.flappy_parfums;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class    MainActivity extends AppCompatActivity {

    // Botones
    private ImageView btnJugar;
    private ImageView btnPersonaje;
    private ImageView personaje;

    // Personaje elegido
    int personajeElegido = R.drawable.personaje_phantom;

    // SharedPreferences para el desbloqueo
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private static final String PREFS_NAME = "MisPuntuaciones";
    private static final String KEY_UNLOCK_LEVEL = "unlockLevel";

    // 0 -> Phantom, 1 -> Azzaro desbloqueado, 2 -> Stronger desbloqueado
    private int unlockLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Inicializamos las prefs
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        unlockLevel = prefs.getInt(KEY_UNLOCK_LEVEL, 0);

        SharedPreferences sp = getSharedPreferences("MisPuntuaciones", MODE_PRIVATE);
        personajeElegido = sp.getInt("personajeSeleccionado", R.drawable.personaje_phantom);

        btnJugar = findViewById(R.id.btnJugar);
        btnPersonaje = findViewById(R.id.btnPersonaje);
        personaje = findViewById(R.id.imgPersonaje);

        // Botón para iniciar juego
        btnJugar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, JuegoActivity.class);
                intent.putExtra("personaje", personajeElegido);
                startActivity(intent);

                // Transición entre las actividades
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                // Animación del botón
                animarBoton(btnJugar);
                reproducirAudio(R.raw.start);
            }
        });

        // Botón para abrir el diálogo de selección de personaje
        btnPersonaje.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animarBoton(btnPersonaje);
                reproducirAudio(R.raw.start);
                mostrarDialogoPersonajes();
            }
        });

        // Animación inicial de la colonia seleccionada
        animarColonia(personajeElegido);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Método para la animación de volar de la colonia, además de animar el personaje
    // Se le pasa el id, que hace referencia a su XML
    private void animarColonia(int idPersonaje) {
        // Agregar frames del personaje
        AnimationDrawable animacionColonia;
        personaje.setBackgroundResource(idPersonaje);

        animacionColonia = (AnimationDrawable) personaje.getBackground();
        animacionColonia.start();

        // Efecto "flotar"
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator volar = ObjectAnimator.ofFloat(personaje, "translationY", -20f, 20f);
        volar.setDuration(400);
        volar.setRepeatCount(ObjectAnimator.INFINITE);
        volar.setRepeatMode(ObjectAnimator.REVERSE);

        set.play(volar);
        set.start();
    }

    // Método para la animación de click en el botón
    private void animarBoton(ImageView idBoton) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator pulsarBoton = ObjectAnimator.ofFloat(idBoton, "translationY", 10, 0);
        pulsarBoton.setDuration(100);

        set.play(pulsarBoton);
        set.start();
    }

    // Método para reproducir sonidos
    public void reproducirAudio(int idAudio) {
        MediaPlayer audio = MediaPlayer.create(this, idAudio);
        if(audio != null) {
            audio.setOnCompletionListener(mp -> mp.release());
            audio.start();
        }
    }

    // Método para mostrar el diálogo de selección de personajes
    private void mostrarDialogoPersonajes(){
        // Construir el diálogo
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.seleccion_personaje, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        ImageView imagenPhantom  = dialogView.findViewById(R.id.imagenPhantom);
        ImageView imagenAzzaro   = dialogView.findViewById(R.id.imagenAzzaro);
        ImageView imagenStronger = dialogView.findViewById(R.id.imagenStronger);

        LinearLayout layoutPhantom  = dialogView.findViewById(R.id.personajePhantomElegido);
        LinearLayout layoutAzzaro   = dialogView.findViewById(R.id.personajeAzzaroElegido);
        LinearLayout layoutStronger = dialogView.findViewById(R.id.personajeStrongerElegido);

        // Habilitar/deshabilitar según unlockLevel
        // Phantom siempre habilitado
        layoutPhantom.setEnabled(true);
        layoutPhantom.setAlpha(1f);

        if (unlockLevel >= 1) {
            // Azzaro desbloqueado
            layoutAzzaro.setEnabled(true);
            layoutAzzaro.setAlpha(1f);
        } else {
            // Azzaro bloqueado
            layoutAzzaro.setEnabled(false);
            layoutAzzaro.setAlpha(0.5f);
        }

        if (unlockLevel >= 2) {
            // Stronger desbloqueado
            layoutStronger.setEnabled(true);
            layoutStronger.setAlpha(1f);
        } else {
            // Stronger bloqueado
            layoutStronger.setEnabled(false);
            layoutStronger.setAlpha(0.5f);
        }

        // Verificar cual es el personaje que ya está seleccionado por defecto, para que,
        // cuando se abra el diálgoo, salga ya con el fondo cambiado
        if(personajeElegido == R.drawable.personaje_phantom) {
            quitarFondo(dialogView, imagenPhantom);
        } else if(personajeElegido == R.drawable.personaje_stronger) {
            quitarFondo(dialogView, imagenStronger);
        } else if(personajeElegido == R.drawable.personaje_azzaro) {
            quitarFondo(dialogView, imagenAzzaro);
        }

        // Eventos de Click de las 3 colonias en el diálogo
        layoutPhantom.setOnClickListener(v -> {
            personajeElegido = R.drawable.personaje_phantom;
            animarColonia(personajeElegido);
            reproducirAudio(R.raw.swich);
            quitarFondo(dialogView, imagenPhantom);
            guardarPersonajeSeleccionado(personajeElegido);
        });

        layoutAzzaro.setOnClickListener(v -> {
            if (unlockLevel >= 1) {
                personajeElegido = R.drawable.personaje_azzaro;
                animarColonia(personajeElegido);
                reproducirAudio(R.raw.swich);
                quitarFondo(dialogView, imagenAzzaro);
                guardarPersonajeSeleccionado(personajeElegido);
            }
        });

        layoutStronger.setOnClickListener(v -> {
            if (unlockLevel >= 2) {
                personajeElegido = R.drawable.personaje_stronger;
                animarColonia(personajeElegido);
                reproducirAudio(R.raw.swich);
                quitarFondo(dialogView, imagenStronger);
                guardarPersonajeSeleccionado(personajeElegido);
            }
        });

        dialog.show();
        // Se cambia el tamaño del diálogo
        int width = (int)(getResources().getDisplayMetrics().density * 300);
        int height = (int)(getResources().getDisplayMetrics().density * 420);
        dialog.getWindow().setLayout(width, height);

        // Botón para cerrar el diálogo
        ImageView btnOK=dialogView.findViewById(R.id.btnOK);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reproducirAudio(R.raw.tap);
                dialog.dismiss();
            }
        });
    }

    // Método para deseleccionar todas las colonias y poner el efecto solo en la elegida
    private void quitarFondo(View dialogView, ImageView imagenSeleccionada) {
        ImageView imagenPhantom  = dialogView.findViewById(R.id.imagenPhantom);
        ImageView imagenAzzaro   = dialogView.findViewById(R.id.imagenAzzaro);
        ImageView imagenStronger = dialogView.findViewById(R.id.imagenStronger);

        // Quitamos el fondo a todas
        imagenPhantom.setBackgroundColor(Color.TRANSPARENT);
        imagenAzzaro.setBackgroundColor(Color.TRANSPARENT);
        imagenStronger.setBackgroundColor(Color.TRANSPARENT);

        // Se añade el fondo con borde a la colonia que está seleccionada
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(Color.parseColor("#C7C286"));
        drawable.setStroke(10, Color.parseColor("#ada86c"));
        imagenSeleccionada.setBackground(drawable);
    }

    // Método para guardar el personaje que el usuario seleccionó al volver a la actividad
    private void guardarPersonajeSeleccionado(int personajeId) {
        SharedPreferences sp = getSharedPreferences("MisPuntuaciones", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("personajeSeleccionado", personajeId);
        editor.apply();
    }

    // Mostrar la transición también al volver a la actividad
    @Override
    public void finish () {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
