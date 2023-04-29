class Writer {

    public static void main(String[] args) {
        try {
            Buffer buffer = new Buffer(4);

            int c = 0;
            if (c == 0)
                buffer = new Buffer(1);

            Reader r1 = new Reader(buffer);
            Reader r2 = new Reader(buffer);
            r1.start();
            r2.start();
            
            while (buffer.notDone()) {
                synchronized(buffer) {
                    buffer.write();
                    buffer.notifyAll();
                }
            }
            r2.join();
            r1.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Reader extends Thread {
    Buffer buffer;

    public Reader(Buffer b) {
        buffer = b;
    }

    public void run() {
        try {
            while (buffer.notDone()) {

                while (buffer.isEmpty()) {
                    buffer.wait();
                }
                buffer.read();

            }
        } catch (Exception e) {

        }
    }
}

class Buffer {
    int count;
    int n;

    Buffer(int _n) {
        count = 0;
        n = _n;
    }
    public void write() {
        this.count++;
    }
    public int read() {
        return count;
    }
    public boolean notDone() {
        if (this.count < this.n)
            return true;
        return false;
    }
    public boolean isEmpty() {
        if (count == 0)
            return true;
        return false;
    }
}

class P1 {}