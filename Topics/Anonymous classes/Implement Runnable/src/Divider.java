class Divider {

    /**
     * Divide a by b. It executes the specified callback to process results
     */
    public static void divide(int a, int b, Callback callback) {

        if (b == 0) {
            callback.failed("Division by zero!");
            return;
        }

        callback.calculated(a / b);
    }
}
