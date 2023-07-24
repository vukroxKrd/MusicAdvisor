public interface Callback {
    /**
     * Takes a result and processes it
     */
    void calculated(int result);

    /**
     * Takes an error message
     */
    void failed(String errorMsg);
}