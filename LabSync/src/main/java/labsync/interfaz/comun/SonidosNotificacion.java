package labsync.interfaz.comun;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Sonidos breves generados localmente, sin archivos de audio externos. */
public final class SonidosNotificacion {
    private static final float MUESTRAS_POR_SEGUNDO = 16_000f;

    private SonidosNotificacion() { }

    public static void reservaAprobada() {
        reproducirEnSegundoPlano(new int[]{660, 880}, new int[]{110, 170});
    }

    public static void reservaRechazada() {
        reproducirEnSegundoPlano(new int[]{520, 350}, new int[]{140, 220});
    }

    public static void solicitudPendiente() {
        reproducirEnSegundoPlano(new int[]{740, 740}, new int[]{90, 120});
    }

    private static void reproducirEnSegundoPlano(int[] frecuencias, int[] duraciones) {
        Thread hilo = new Thread(() -> reproducir(frecuencias, duraciones),
                "labsync-sonido-notificacion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static void reproducir(int[] frecuencias, int[] duraciones) {
        AudioFormat formato = new AudioFormat(MUESTRAS_POR_SEGUNDO, 8, 1, true, false);
        try (SourceDataLine salida = AudioSystem.getSourceDataLine(formato)) {
            salida.open(formato);
            salida.start();
            for (int i = 0; i < frecuencias.length; i++) {
                escribirTono(salida, frecuencias[i], duraciones[i]);
                escribirSilencio(salida, 45);
            }
            salida.drain();
        } catch (Exception ex) {
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    private static void escribirTono(SourceDataLine salida, int frecuencia, int milisegundos) {
        int total = Math.round(MUESTRAS_POR_SEGUNDO * milisegundos / 1000f);
        byte[] datos = new byte[total];
        for (int i = 0; i < total; i++) {
            double envolvente = Math.min(1d, Math.min(i / 100d, (total - i) / 180d));
            datos[i] = (byte) (Math.sin(2d * Math.PI * frecuencia * i
                    / MUESTRAS_POR_SEGUNDO) * 45d * envolvente);
        }
        salida.write(datos, 0, datos.length);
    }

    private static void escribirSilencio(SourceDataLine salida, int milisegundos) {
        byte[] silencio = new byte[Math.round(MUESTRAS_POR_SEGUNDO * milisegundos / 1000f)];
        salida.write(silencio, 0, silencio.length);
    }
}
