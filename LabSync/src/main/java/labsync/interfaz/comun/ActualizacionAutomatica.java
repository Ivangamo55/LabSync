package labsync.interfaz.comun;

import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import javax.swing.Timer;

/** Ejecuta lecturas periódicas fuera del EDT y publica sus resultados en el EDT. */
public final class ActualizacionAutomatica<T> {
    private static final Logger LOGGER = Logger.getLogger(ActualizacionAutomatica.class.getName());

    private final Window ventana;
    private final Supplier<T> consulta;
    private final Consumer<T> aplicar;
    private final Timer temporizador;
    private final AtomicBoolean actualizando = new AtomicBoolean();
    private SwingWorker<T, Void> trabajador;

    public ActualizacionAutomatica(Window ventana, int intervaloMs,
            Supplier<T> consulta, Consumer<T> aplicar) {
        this.ventana = ventana;
        this.consulta = consulta;
        this.aplicar = aplicar;
        temporizador = new Timer(intervaloMs, event -> actualizar());
        temporizador.setInitialDelay(intervaloMs);
        instalarCicloDeVida();
        temporizador.start();
    }

    public void actualizar() {
        if (!ventana.isDisplayable() || !ventana.isVisible()
                || !actualizando.compareAndSet(false, true)) {
            return;
        }
        trabajador = new SwingWorker<>() {
            @Override
            protected T doInBackground() {
                return consulta.get();
            }

            @Override
            protected void done() {
                try {
                    if (!isCancelled() && ventana.isDisplayable() && ventana.isVisible()) {
                        aplicar.accept(get());
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    LOGGER.log(Level.WARNING, "No fue posible actualizar "
                            + ventana.getClass().getSimpleName(), ex.getCause());
                } finally {
                    actualizando.set(false);
                }
            }
        };
        trabajador.execute();
    }

    public void detener() {
        temporizador.stop();
        SwingWorker<T, Void> activo = trabajador;
        if (activo != null && !activo.isDone()) activo.cancel(true);
        actualizando.set(false);
    }

    private void instalarCicloDeVida() {
        ventana.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { detener(); }
            @Override public void windowClosed(WindowEvent event) { detener(); }
        });
        ventana.addComponentListener(new ComponentAdapter() {
            @Override public void componentHidden(ComponentEvent event) { detener(); }
            @Override public void componentShown(ComponentEvent event) {
                if (!temporizador.isRunning()) temporizador.start();
            }
        });
    }
}
