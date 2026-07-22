package labsync.interfaz.comun;

import labsync.aplicacion.AplicacionLabSync;

/** Reglas compartidas para todos los calendarios de AplicacionLabSync. */
public final class ValidacionFechas {

    private ValidacionFechas() {
    }

    public static void bloquearFinesDeSemana(com.toedter.calendar.JDateChooser selector) {
        selector.getJCalendar().getDayChooser().addDateEvaluator(new EvaluadorFinDeSemana());
    }

    public static boolean esFinDeSemana(java.util.Date fecha) {
        if (fecha == null) return false;
        return esFinDeSemana(fecha.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
    }

    public static boolean esFinDeSemana(java.time.LocalDate fecha) {
        java.time.DayOfWeek dia = fecha.getDayOfWeek();
        return dia == java.time.DayOfWeek.SATURDAY || dia == java.time.DayOfWeek.SUNDAY;
    }

    public static java.util.Date siguienteDiaHabil(java.util.Date fecha) {
        java.time.LocalDate dia = fecha.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        while (esFinDeSemana(dia)) dia = dia.plusDays(1);
        return java.sql.Date.valueOf(dia);
    }

    public static java.util.Date anteriorDiaHabil(java.util.Date fecha) {
        java.time.LocalDate dia = fecha.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        while (esFinDeSemana(dia)) dia = dia.minusDays(1);
        return java.sql.Date.valueOf(dia);
    }

    public static boolean validarDiaHabil(java.awt.Component padre, java.util.Date fecha, String accion) {
        if (!esFinDeSemana(fecha)) return true;
        javax.swing.JOptionPane.showMessageDialog(
                padre,
                "No se pueden " + accion + " en sábado o domingo.",
                "Fin de semana no permitido",
                javax.swing.JOptionPane.WARNING_MESSAGE);
        return false;
    }

    public static boolean validarHorarioFuturo(
            java.awt.Component padre, java.time.LocalDate fecha, String horario) {
        if (!fecha.equals(java.time.LocalDate.now())) return true;

        String horaInicio = horario.substring(0, horario.indexOf(" - "));
        java.time.LocalTime inicio = java.time.LocalTime.parse(
                horaInicio, java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        if (inicio.isAfter(java.time.LocalTime.now())) return true;

        javax.swing.JOptionPane.showMessageDialog(
                padre,
                "No puedes reservar este horario porque su hora de inicio ya pasó.",
                "Horario no válido",
                javax.swing.JOptionPane.WARNING_MESSAGE);
        return false;
    }

    private static final class EvaluadorFinDeSemana implements com.toedter.calendar.IDateEvaluator {
        @Override public boolean isSpecial(java.util.Date date) { return false; }
        @Override public java.awt.Color getSpecialForegroundColor() { return null; }
        @Override public java.awt.Color getSpecialBackroundColor() { return null; }
        @Override public String getSpecialTooltip() { return null; }
        @Override public boolean isInvalid(java.util.Date date) { return esFinDeSemana(date); }
        @Override public java.awt.Color getInvalidForegroundColor() { return java.awt.Color.GRAY; }
        @Override public java.awt.Color getInvalidBackroundColor() { return new java.awt.Color(238, 238, 238); }
        @Override public String getInvalidTooltip() { return "No disponible en sábado o domingo"; }
    }
}
