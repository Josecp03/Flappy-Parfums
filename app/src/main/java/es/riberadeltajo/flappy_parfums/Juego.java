package es.riberadeltajo.flappy_parfums;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

public class Juego extends SurfaceView implements SurfaceHolder.Callback {
    private BucleJuego bucle;
    // Personaje
    private Bitmap[] framesPersonaje;
    private Bitmap gameOverBitmap;
    private Bitmap dialogoScoreBitmap; // Recuadro de score (para cuando se pierde)
    private Bitmap restartBitmap;      // Botón de reinicio (restart.png)
    private Bitmap menuBitmap;         // Botón para ir al menú (menu.png)

    //Variables para animar el personaje
    private int frameIndex = 0;
    private long ultimoCambioFrame = 0;
    private final int DURACION_FRAME = 100;
    private float posPersonajeY;
    private float velY = 0;
    private final float GRAVEDAD = 3f;
    private final float SALTO = -30;
    private int score = 0;
    private int maxTuberias = 15;
    private boolean gano = false;

    // Suelo
    private Bitmap suelo;
    private float posSuelo1;
    private float posSuelo2;
    private final float velSuelo = 7f;
    private float sueloY;

    //Variables para generar las tuberías
    private ArrayList<Tuberia> tuberias;
    private final float TIEMPO_ENTRE_TUBERIAS = 600f;
    private boolean gameOver = false;

    //Dimensiones del persoanje
    private int personajeAncho;
    private int personajeAlto;
    private Rect rectPersonaje;

    // Power-up
    private int totalTuberiasGeneradas = 0;
    private int invisibilidadTuberiasRestantes = 0;

    // Estado del juego
    private boolean gameStarted = false;
    private boolean deathSoundPlayed = false;

    // Imagen de inicio
    private Bitmap imagenHola;

    // Animación "volar"
    private AnimatorSet animSet;

    // SharedPreferences para la mejor puntuación
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "MisPuntuaciones";
    private static final String KEY_BEST_SCORE = "mejorPuntuacion";
    private int bestScore = 0;

    //Variables para el desbloqueo de los personajes
    private int unlockLevel;             // Indica cuántos personajes están desbloqueados (0,1,2)
    private int idPersonajeSeleccionado; // Guardamos qué personaje se ha elegido

    public Juego(Context context, int idPersonaje) {
        super(context);
        getHolder().addCallback(this);
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);

        //Cargar las preferencias del desbloqueo de los niveles
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        unlockLevel = sp.getInt("unlockLevel", 0);
        SharedPreferences preferences = context.getSharedPreferences("MisPuntuaciones", Context.MODE_PRIVATE);
        this.idPersonajeSeleccionado = idPersonaje;

        //Cargar las preferencias del personaje seleccionado
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("personajeSeleccionado", idPersonajeSeleccionado);
        editor.apply();

        //Establecer el personaje seleccionado
        establecerPersonaje(idPersonajeSeleccionado);


        // Según el personaje y el unlockLevel, ajustamos el maxTuberias
        if (idPersonajeSeleccionado == R.drawable.personaje_phantom) {
            // Fase Phantom: si unlockLevel=0, meta=15; si ya está desbloqueado (>=1), sin límite
            if (unlockLevel == 0) {
                maxTuberias = 15;
            } else {
                maxTuberias = 999999; // sin límite
            }
        } else if (idPersonajeSeleccionado == R.drawable.personaje_azzaro) {
            // Fase Azzaro: si unlockLevel=1, meta=20; si unlockLevel=2, sin límite
            if (unlockLevel == 1) {
                maxTuberias = 20;
            } else {
                maxTuberias = 999999;
            }
        } else if (idPersonajeSeleccionado == R.drawable.personaje_stronger) {
            // Fase Stronger: sin límite
            maxTuberias = 999999;
        }

        // Escalar frames del personaje
        for (int i = 0; i < framesPersonaje.length; i++) {
            framesPersonaje[i] = Bitmap.createScaledBitmap(
                    framesPersonaje[i],
                    framesPersonaje[i].getWidth() / 13,
                    framesPersonaje[i].getHeight() / 13,
                    true
            );
        }
        personajeAncho = framesPersonaje[0].getWidth();
        personajeAlto = framesPersonaje[0].getHeight();

        suelo = BitmapFactory.decodeResource(getResources(), R.drawable.suelo);
        posPersonajeY = 0;

        tuberias = new ArrayList<>();
        rectPersonaje = new Rect();

        imagenHola = BitmapFactory.decodeResource(getResources(), R.drawable.message);

        //Cargar las imagenes
        gameOverBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.gameover);
        dialogoScoreBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.dialogo_score);
        restartBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.restart);
        menuBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.menu);
        menuBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.menu);

        // Escalar el mensaje de Gameover
        // Esto es el 40% de su tamaño original
        float scaleOver = 0.9f;
        int newWidthOver = (int)(gameOverBitmap.getWidth() * scaleOver);
        int newHeightOver = (int)(gameOverBitmap.getHeight() * scaleOver);
        gameOverBitmap = Bitmap.createScaledBitmap(gameOverBitmap, newWidthOver, newHeightOver, true);

        // Escalar el diálogo de puntos
        // Esto es el 40% de su tamaño original
        float scaleFactorDialog = 1.25f;
        int newWidthDialog = (int)(dialogoScoreBitmap.getWidth() * scaleFactorDialog);
        int newHeightDialog = (int)(dialogoScoreBitmap.getHeight() * scaleFactorDialog);
        dialogoScoreBitmap = Bitmap.createScaledBitmap(dialogoScoreBitmap, newWidthDialog, newHeightDialog, true);

        // Escalar el botón de START
        // Esto es el 30% de su tamaño original
        float scaleFactor = 0.3f;
        int newWidth = (int)(menuBitmap.getWidth() * scaleFactor);
        int newHeight = (int)(menuBitmap.getHeight() * scaleFactor);
        menuBitmap = Bitmap.createScaledBitmap(menuBitmap, newWidth, newHeight, true);

        // Escalar el botón de RESTART
        // Esto es el 30% de su tamaño original
        float scaleFactorRestart = 0.3f;
        int newWidthRestart = (int)(restartBitmap.getWidth() * scaleFactorRestart);
        int newHeightRestart = (int)(restartBitmap.getHeight() * scaleFactorRestart);
        restartBitmap = Bitmap.createScaledBitmap(restartBitmap, newWidthRestart, newHeightRestart, true);

        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        bestScore = prefs.getInt(KEY_BEST_SCORE, 0);


    }

    // Getters y Setters para la propiedad que animaremos
    public float getPosPersonajeY() {
        return posPersonajeY;
    }
    public void setPosPersonajeY(float pos) {
        this.posPersonajeY = pos;
    }


    //Metodo que establece los frames del personaje según el personaje seleccionado
    private void establecerPersonaje(int idPersonaje) {
        if (idPersonaje == R.drawable.personaje_phantom) {
            framesPersonaje = new Bitmap[4];
            framesPersonaje[0] = BitmapFactory.decodeResource(getResources(), R.drawable.phantom1);
            framesPersonaje[1] = BitmapFactory.decodeResource(getResources(), R.drawable.phantom2);
            framesPersonaje[2] = BitmapFactory.decodeResource(getResources(), R.drawable.phantom3);
            framesPersonaje[3] = BitmapFactory.decodeResource(getResources(), R.drawable.phantom4);
        } else if (idPersonaje == R.drawable.personaje_azzaro) {
            framesPersonaje = new Bitmap[5];
            framesPersonaje[0] = BitmapFactory.decodeResource(getResources(), R.drawable.azzaro1);
            framesPersonaje[1] = BitmapFactory.decodeResource(getResources(), R.drawable.azzaro2);
            framesPersonaje[2] = BitmapFactory.decodeResource(getResources(), R.drawable.azzaro3);
            framesPersonaje[3] = BitmapFactory.decodeResource(getResources(), R.drawable.azzaro4);
            framesPersonaje[4] = BitmapFactory.decodeResource(getResources(), R.drawable.azzaro5);
        } else if (idPersonaje == R.drawable.personaje_stronger) {
            framesPersonaje = new Bitmap[4];
            framesPersonaje[0] = BitmapFactory.decodeResource(getResources(), R.drawable.stronger1);
            framesPersonaje[1] = BitmapFactory.decodeResource(getResources(), R.drawable.stronger2);
            framesPersonaje[2] = BitmapFactory.decodeResource(getResources(), R.drawable.stronger3);
            framesPersonaje[3] = BitmapFactory.decodeResource(getResources(), R.drawable.stronger4);
        } else {
            // Por defecto Phantom
            framesPersonaje = new Bitmap[4];
            framesPersonaje[0] = BitmapFactory.decodeResource(getResources(), R.drawable.phantom1);
            framesPersonaje[1] = BitmapFactory.decodeResource(getResources(), R.drawable.phantom2);
            framesPersonaje[2] = BitmapFactory.decodeResource(getResources(), R.drawable.phantom3);
            framesPersonaje[3] = BitmapFactory.decodeResource(getResources(), R.drawable.phantom4);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        int pantallaAncho = getWidth();
        int pantallaAlto = getHeight();
        int anchoOriginal = suelo.getWidth();
        int altoOriginal = suelo.getHeight();
        float factorEscala = (float) pantallaAncho / anchoOriginal;
        int nuevoAncho = pantallaAncho;
        int nuevoAlto = (int) (altoOriginal * factorEscala);
        suelo = Bitmap.createScaledBitmap(suelo, nuevoAncho, nuevoAlto, true);
        sueloY = pantallaAlto - suelo.getHeight();
        posSuelo1 = 0;
        posSuelo2 = suelo.getWidth();
        posPersonajeY = (pantallaAlto - personajeAlto) / 2f;


        //Animacion de la pantalla inicial del personaje flotando
        animSet = new AnimatorSet();
        ObjectAnimator volar = ObjectAnimator.ofFloat(this, "posPersonajeY", posPersonajeY - 40, posPersonajeY);
        volar.setDuration(400);
        volar.setRepeatCount(ObjectAnimator.INFINITE);
        volar.setRepeatMode(ObjectAnimator.REVERSE);
        animSet.play(volar);
        animSet.start();

        //Iniciar el bucle
        bucle = new BucleJuego(holder, this);
        bucle.setEnEjecucion(true);
        bucle.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        bucle.setEnEjecucion(false);
        while (retry) {
            try {
                bucle.join();
                retry = false;
            } catch (InterruptedException e) { }
        }
    }


    //Metodo que actualiza la lógica del juego
    public void actualizar() {
        //Actualiza la posicion del suelo si el juego no ha terminado
        if (!gameOver) {
            posSuelo1 -= velSuelo;
            posSuelo2 -= velSuelo;
            // Si el primer trozo del suelo sale completamente de la pantalla, lo reposiciona detrás del segundo
            if (posSuelo1 + suelo.getWidth() < 0) {
                posSuelo1 = posSuelo2 + suelo.getWidth();
            }
            // Si el segundo trozo del suelo sale completamente de la pantalla, lo reposiciona detrás del primero
            if (posSuelo2 + suelo.getWidth() < 0) {
                posSuelo2 = posSuelo1 + suelo.getWidth();
            }
        }

        //Si el juego no ha comenzado, solo se actualiza la animación del personaje
        if (!gameStarted) {
            long ahora = System.currentTimeMillis();
            if (ahora - ultimoCambioFrame >= DURACION_FRAME) {
                frameIndex = (frameIndex + 1) % framesPersonaje.length;
                ultimoCambioFrame = ahora;
            }
            return;
        }

        //Si el juego ha termiando, reproduce el sonido de muerte
        if (gameOver) {
            if (!gano && !deathSoundPlayed) {
                reproducirAudio(R.raw.morir);
                deathSoundPlayed = true;
            }
            return;
        }

        //Aplica la gravedad al personaje y actualiza su posición vertical
        velY += GRAVEDAD;
        posPersonajeY += velY;

        //Actualiza la animación del personaje
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoCambioFrame >= DURACION_FRAME) {
            frameIndex = (frameIndex + 1) % framesPersonaje.length;
            ultimoCambioFrame = ahora;
        }

        // Limita la posición del personaje para evitar que salga de los límites de la pantalla
        if (posPersonajeY <= 0) {
            posPersonajeY = 0;
            gameOver = true;
        }

        // Calcula el límite inferior de la pantalla
        float limiteInferior = getHeight() - suelo.getHeight() - personajeAlto;
        if (posPersonajeY >= limiteInferior) {
            posPersonajeY = limiteInferior;
            gameOver = true;
        }

        //Define las coordenadas del rectángulo que representa al personaje
        float personajeX = 100;
        rectPersonaje.set((int) personajeX, (int) posPersonajeY,
                (int) (personajeX + personajeAncho), (int) (posPersonajeY + personajeAlto));

        //Genera nuevas tuberias
        generarTuberias();
        //Actualiza las tuberias existentes y elimina las que estan fuera de la pantalla
        actualizarTuberias();
        //Comprueba si el personaje colisiona por alguna tubería
        chequearColisiones();
        //Comprubea si el personaje recoge el power-up
        chequearPowerUp();
        //Comprueba que el personaje haya pasado por una tubería y actualiza la puntuación
        chequearPasoTuberias();

        //Actualiza cada tubería individualmente
        for (Tuberia t : tuberias) {
            t.setVelocidad(velSuelo);
            t.actualizar();
        }
    }

    public void renderizar(Canvas canvas) {
        if (canvas == null) return;
        //Limpia el lienzo antes de dibujar
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        //Si el juego no ha comenzado, dibuja la pantalla de inicio
        if (!gameStarted) {
            float personajeX = 100;
            Bitmap frameActual = framesPersonaje[frameIndex];
            canvas.drawBitmap(frameActual, personajeX, posPersonajeY, null);

            float holaX = (getWidth() - imagenHola.getWidth()) / 2f;
            float holaY = (getHeight() - imagenHola.getHeight()) / 2f;
            canvas.drawBitmap(imagenHola, holaX, holaY, null);

            renderizarSuelo(canvas);
            return;
        }

        float personajeX = 100;
        //Itera sobre las tuberías y las dibuja en el lienzo
        for (Tuberia t : new ArrayList<>(tuberias)) {
            t.dibujar(canvas);
        }
        renderizarSuelo(canvas);

        //Si el juego no ha terminado, muestra la puntuación actual
        if (!gameOver) {
            Paint paint = new Paint();
            Typeface typeface = ResourcesCompat.getFont(getContext(), R.font.numbers);
            paint.setTypeface(typeface);
            paint.setTextSize(120);

            // POSICIONAR LOS PUNTOS JUSTO EN EL CENTRO DE LA PANTALLA
            // Preparar el texto a mostrar
            String scoreText = String.valueOf(score);

            // Calcular el ancho del texto
            float textWidth = paint.measureText(scoreText);

            // Calcular la posición central horizontal
            float centerX = getWidth() / 2f;
            float textX = centerX - textWidth / 2f;

            // Configurar sombra
            paint.setShadowLayer(1, 10, 10, Color.BLACK);

            // Dibujar el trazo (stroke)
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(12);
            paint.setColor(Color.BLACK);
            canvas.drawText(scoreText, textX, 300, paint);

            // Dibujar el relleno (fill)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawText(scoreText, textX, 300, paint);

        }

        //Dibuja al personaje con transparencia si tiene power-up activo
        Bitmap frameActual = framesPersonaje[frameIndex];
        Paint personajePaint = new Paint();
        if (invisibilidadTuberiasRestantes > 0) {
            personajePaint.setAlpha(100);
        } else {
            personajePaint.setAlpha(255);
        }
        canvas.drawBitmap(frameActual, personajeX, posPersonajeY, personajePaint);

        //Si el juego ha terminado, muestra el resultado final
        if (gameOver) {
            // Actualizamos bestScore siempre (tanto si se gana como si se pierde)
            if (score > bestScore) {
                bestScore = score;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(KEY_BEST_SCORE, bestScore);
                editor.apply();
            }
            if (gano) {
                // Rama de victoria: dibujar mensaje y botón de menú
                Paint paint = new Paint();
                paint.setColor(Color.WHITE);
                paint.setTextSize(100);
                paint.setShadowLayer(1, 10, 10, Color.BLACK);
                paint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.numbers));

                String mensaje = "¡Ganaste!";
                float textWidth = paint.measureText(mensaje);
                float x = (getWidth() - textWidth) / 2f;
                float y = (getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);
                canvas.drawText(mensaje, x, y, paint);

                float menuMarginWin = 30f;
                float menuX = (getWidth() - menuBitmap.getWidth()) / 2f;
                float menuY = y + 100f + menuMarginWin;
                canvas.drawBitmap(menuBitmap, menuX, menuY, null);

            } else {
                // Rama de derrota: dibujar recuadro, puntuación y botones
                float margenEntreImagenes = 20f;
                float alturaTotal = gameOverBitmap.getHeight() + margenEntreImagenes + dialogoScoreBitmap.getHeight();
                float bloqueY = (getHeight() - alturaTotal) / 2f - 100f;
                float gameOverX = (getWidth() - gameOverBitmap.getWidth()) / 2f;
                canvas.drawBitmap(gameOverBitmap, gameOverX, bloqueY, null);

                float dialogoX = (getWidth() - dialogoScoreBitmap.getWidth()) / 2f;
                // El +30 se pone para que no esté tan pegado al texto de Game Over
                float dialogoY = bloqueY + gameOverBitmap.getHeight() + margenEntreImagenes + 40;
                canvas.drawBitmap(dialogoScoreBitmap, dialogoX, dialogoY, null);


                // DIBUJAR PUNTOS PARTIDA
                Paint paintScore = new Paint();
                paintScore.setTypeface(ResourcesCompat.getFont(getContext(), R.font.numbers));
                paintScore.setTextSize(55);
                String finalScore = String.valueOf(score);

                // Definimos un margen opcional (por ejemplo, 10 píxeles)
                float margin = 80f;
                float textWidthScore = paintScore.measureText(finalScore);
                float textXScore = (dialogoX + dialogoScoreBitmap.getWidth()) - textWidthScore - margin;
                float offsetYScore = dialogoScoreBitmap.getHeight() * 0.37f;
                float scoreCenterY = dialogoY + offsetYScore;
                float textYScore = scoreCenterY - ((paintScore.descent() + paintScore.ascent()) / 2f);

                // Dibujar trazo para los puntos de la partida
                paintScore.setStyle(Paint.Style.STROKE);
                paintScore.setStrokeWidth(12);
                paintScore.setColor(Color.BLACK);
                canvas.drawText(finalScore, textXScore, textYScore, paintScore);

                // Dibujar relleno para los puntos de la partida
                paintScore.setStyle(Paint.Style.FILL);
                paintScore.setColor(Color.WHITE);
                canvas.drawText(finalScore, textXScore, textYScore, paintScore);


                // DIBUJAR PUNTOS MÁXIMOS (bestScore) con trazo
                Paint paintBest = new Paint();
                paintBest.setTypeface(ResourcesCompat.getFont(getContext(), R.font.numbers));
                paintBest.setTextSize(55);
                String finalBest = String.valueOf(bestScore);

                float textWidthScoreBest = paintScore.measureText(finalBest);
                float marginBest = 80f;
                float textXScoreBest = (dialogoX + dialogoScoreBitmap.getWidth()) - textWidthScoreBest - marginBest;
                float offsetYScoreBest = dialogoScoreBitmap.getHeight() * 0.75f;
                float bestCenterY = dialogoY + offsetYScoreBest;
                float textYScoreBest = bestCenterY - ((paintBest.descent() + paintBest.ascent()) / 2f);

                // Crear un paint para el trazo
                Paint strokePaint = new Paint(paintBest);
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setStrokeWidth(12);
                strokePaint.setColor(Color.BLACK);
                canvas.drawText(finalBest, textXScoreBest, textYScoreBest, strokePaint);

                // Crear un paint para el relleno
                Paint fillPaint = new Paint(paintBest);
                fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setColor(Color.WHITE);
                canvas.drawText(finalBest, textXScoreBest, textYScoreBest, fillPaint);

                // Dibujar botones restart y menú
                float offsetRestart = 30f;
                float verticalOffset = 50f;
                float restartX = dialogoX + (dialogoScoreBitmap.getWidth() - restartBitmap.getWidth()) / 2f - offsetRestart;
                float restartY = dialogoY + (dialogoScoreBitmap.getHeight() - restartBitmap.getHeight()) / 2f - verticalOffset;
                canvas.drawBitmap(restartBitmap, restartX, restartY, null);

                float menuMargin = 20f;
                float offsetMenu = 30f;
                float menuX = dialogoX + (dialogoScoreBitmap.getWidth() - menuBitmap.getWidth()) / 2f - offsetMenu;
                float menuY = restartY + restartBitmap.getHeight() + menuMargin;
                canvas.drawBitmap(menuBitmap, menuX, menuY, null);
            }
        }
    }

    //Metodo para generar las tuberías
    private void generarTuberias() {
        //Si el jugador ha alcanzado el máximo número de tuberías permitidas , no generamos más
        if (score >= maxTuberias - 2) return;
        float anchoPantalla = getWidth();

        //Si no hay ninguna tubería en la lista, creamos la primera
        if (tuberias.isEmpty()) {
            totalTuberiasGeneradas++;
            boolean esPowerUp = (totalTuberiasGeneradas == 10);
            tuberias.add(new Tuberia(getContext(), anchoPantalla + TIEMPO_ENTRE_TUBERIAS, sueloY, velSuelo, 350, esPowerUp));
            return;
        }

        //Obtenemos la última tubería generada para calcular su posición
        Tuberia ultimaTuberia = tuberias.get(tuberias.size() - 1);
        //Verificamos si la última tubería está lo suficientemente alejada del borde derecho de la pantalla
        if (ultimaTuberia.getX() + ultimaTuberia.getAncho() < anchoPantalla) {
            totalTuberiasGeneradas++;
            boolean esPowerUp = (totalTuberiasGeneradas == 10);
            float nuevaPosX = ultimaTuberia.getX() + ultimaTuberia.getAncho() + TIEMPO_ENTRE_TUBERIAS;
            tuberias.add(new Tuberia(getContext(), nuevaPosX, sueloY, velSuelo, 350, esPowerUp));
        }
    }

    private void actualizarTuberias() {
        //Iteramos sobre las tuberías desde la última hasta la primera para evitar problemas al eliminar elementos
        for (int i = tuberias.size() - 1; i >= 0; i--) {
            Tuberia t = tuberias.get(i);
            t.actualizar();
            //Si la tubería está fuera de la pantalla, la eliminamos de la lista
            if (t.fueraDePantalla()) {
                tuberias.remove(i);
            }
        }
    }

    private void chequearColisiones() {
        //Si el personaje tiene activado un power-up de invisibilidad, no se comprueban colisiones
        if (invisibilidadTuberiasRestantes > 0) return;
        //Iteramos sobre todas las tuberías para detectar colisiones con el personaje
        for (Tuberia t : tuberias) {
            //Comprobamos si hay colisión entre el personaje y la tubería
            if (t.colisionaCon(rectPersonaje)) {
                if (!gameOver) {
                    gameOver = true;
                }
                break;
            }
        }
    }

    private void chequearPowerUp() {
        //Iteramos sobre todas las tuberías para verificar si el personaje recoge un power-up
        for (Tuberia t : tuberias) {
            //Verificamos si la tubería tiene un power-up, este no ha sido recolectado previamente,
            //y si el rectángulo del personaje intersecta con el rectángulo del power-up
            if (t.hasPowerUp() && !t.isPowerUpCollected() && Rect.intersects(rectPersonaje, t.getRectPowerUp())) {
                t.setPowerUpCollected(true);
                invisibilidadTuberiasRestantes = 4;
            }
        }
    }

    private void chequearPasoTuberias() {
        float personajeX = 100;
        for (Tuberia t : tuberias) {
            float tuberiaXFinal = t.getX() + t.getAncho();
            //Verificamos si la tubería no ha sido marcada como "punto sumado" y si ha pasado completamente al personaje
            if (!t.isPuntoSumado() && tuberiaXFinal < personajeX) {
                score++;
                t.setPuntoSumado(true);
                reproducirAudio(R.raw.point);
                //Si el power-up de invisibilidad está activo, reducimos su duración
                if (invisibilidadTuberiasRestantes > 0) {
                    invisibilidadTuberiasRestantes--;
                }

                // Comprobamos si alcanzamos la meta
                if (score >= maxTuberias) {
                    gano = true;
                    gameOver = true;

                    //Aquí actualizamos el unlockLevel si corresponde
                    SharedPreferences sp2 = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    int currentUnlock = sp2.getInt("unlockLevel", 0);

                    if (currentUnlock == 0 && idPersonajeSeleccionado == R.drawable.personaje_phantom) {
                        // Pasamos de 0 a 1 (desbloqueamos Azzaro)
                        SharedPreferences.Editor editor = sp2.edit();
                        editor.putInt("unlockLevel", 1);
                        editor.apply();
                    } else if (currentUnlock == 1 && idPersonajeSeleccionado == R.drawable.personaje_azzaro) {
                        // Pasamos de 1 a 2 (desbloqueamos Stronger)
                        SharedPreferences.Editor editor = sp2.edit();
                        editor.putInt("unlockLevel", 2);
                        editor.apply();
                    }
                }
                break;
            }
        }
    }

    private void renderizarSuelo(Canvas canvas) {
        //Dibuja los trozos del suelo
        canvas.drawBitmap(suelo, posSuelo1, sueloY, null);
        canvas.drawBitmap(suelo, posSuelo2, sueloY, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (gameOver) {
                // Si se ganó, solo se activa el botón de menú
                if (gano) {
                    float menuMarginWin = 30f;
                    Paint paint = new Paint();
                    paint.setTextSize(100);
                    paint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.numbers));
                    float mensajeWidth = paint.measureText("¡Ganaste!");
                    float mensajeX = (getWidth() - mensajeWidth) / 2f;
                    float mensajeY = (getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2f);

                    float menuX = (getWidth() - menuBitmap.getWidth()) / 2f;
                    float menuY = mensajeY + 100f + menuMarginWin;
                    Rect menuRect = new Rect(
                            (int) menuX,
                            (int) menuY,
                            (int) (menuX + menuBitmap.getWidth()),
                            (int) (menuY + menuBitmap.getHeight())
                    );
                    float touchX = event.getX();
                    float touchY = event.getY();
                    if (menuRect.contains((int) touchX, (int) touchY)) {
                        Intent intent = new Intent(getContext(), MainActivity.class);
                        getContext().startActivity(intent);
                        ((android.app.Activity)getContext()).finish();
                        return true;
                    }
                    return true;
                }
                // Si se perdió, comprobar ambos botones: restart y menú
                else {
                    float margenEntreImagenes = 20f;
                    float alturaTotal = gameOverBitmap.getHeight() + margenEntreImagenes + dialogoScoreBitmap.getHeight();
                    float bloqueY = (getHeight() - alturaTotal) / 2f - 100f;
                    float dialogoX = (getWidth() - dialogoScoreBitmap.getWidth()) / 2f;
                    float dialogoY = bloqueY + gameOverBitmap.getHeight() + margenEntreImagenes;

                    float offsetRestart = 30f;
                    float verticalOffset = 50f;
                    float restartX = dialogoX + (dialogoScoreBitmap.getWidth() - restartBitmap.getWidth()) / 2f - offsetRestart;
                    float restartY = dialogoY + (dialogoScoreBitmap.getHeight() - restartBitmap.getHeight()) / 2f - verticalOffset;
                    Rect restartRect = new Rect(
                            (int) restartX,
                            (int) restartY,
                            (int) (restartX + restartBitmap.getWidth()),
                            (int) (restartY + restartBitmap.getHeight())
                    );

                    float menuMargin = 20f;
                    float offsetMenu = 30f;
                    float menuX = dialogoX + (dialogoScoreBitmap.getWidth() - menuBitmap.getWidth()) / 2f - offsetMenu;
                    float menuY = restartY + restartBitmap.getHeight() + menuMargin;
                    Rect menuRect = new Rect(
                            (int) menuX,
                            (int) menuY,
                            (int) (menuX + menuBitmap.getWidth()),
                            (int) (menuY + menuBitmap.getHeight())
                    );

                    float touchX = event.getX();
                    float touchY = event.getY();
                    if (restartRect.contains((int) touchX, (int) touchY)) {
                        reiniciarPartida();
                        return true;
                    } else if (menuRect.contains((int) touchX, (int) touchY)) {
                        Intent intent = new Intent(getContext(), MainActivity.class);
                        getContext().startActivity(intent);
                        ((android.app.Activity)getContext()).finish();
                        return true;
                    }
                    return true;
                }
            } else if (!gameStarted) {
                gameStarted = true;
                if (animSet != null && animSet.isRunning()) {
                    animSet.cancel();
                }
                velY = SALTO;
                reproducirAudio(R.raw.spray);
            } else {
                velY = SALTO;
                reproducirAudio(R.raw.spray);
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void reiniciarPartida() {
        //Restablecemos los parametros como en el inicio
        score = 0;
        gano = false;
        gameOver = false;
        gameStarted = false;
        deathSoundPlayed = false;
        totalTuberiasGeneradas = 0;
        invisibilidadTuberiasRestantes = 0;
        tuberias.clear();

        posPersonajeY = (getHeight() - personajeAlto) / 2f;
        velY = 0;

        if (animSet != null) {
            animSet.cancel();
        }
        //Creamos una nueva animación para hacer que el personaje "vuela" en el lugar
        animSet = new AnimatorSet();
        ObjectAnimator volar = ObjectAnimator.ofFloat(this, "posPersonajeY", posPersonajeY - 40, posPersonajeY);
        volar.setDuration(400);
        volar.setRepeatCount(ObjectAnimator.INFINITE);
        volar.setRepeatMode(ObjectAnimator.REVERSE);
        animSet.play(volar);
        animSet.start();
    }

    public void reproducirAudio(int idAudio) {
        //Se reproducen los sonidos
        MediaPlayer audio = MediaPlayer.create(getContext(), idAudio);
        if (audio != null) {
            audio.setOnCompletionListener(MediaPlayer::release);
            audio.start();
        }
    }
}
