package ca.sahiljain.chess;

public enum Player {
    MAXIMIZER, MINIMIZER;

    public Player opposite() {
        return this == MAXIMIZER ? MINIMIZER : MAXIMIZER;
    }
}
