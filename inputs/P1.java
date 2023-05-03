//class Writer {
//
//    public static void main(String[] args) {
//        try {
//            Buffer buffer = new Buffer(4);
//
//            int c = 0;
//            if (c == 0)
//                buffer = new Buffer(1);
//
//            Reader r1 = new Reader(buffer);
//            Reader r2 = new Reader(buffer);
//            r1.start();
//            r2.start();
//            
//            while (buffer.notDone()) {
//                synchronized(buffer) {
//                    buffer.write();
//                    buffer.notifyAll();
//                }
//            }
//            r2.join();
//            r1.join();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
//
//class Reader extends Thread {
//    Buffer buffer;
//
//    public Reader(Buffer b) {
//        buffer = b;
//    }
//
//    public void run() {
//        try {
//            while (buffer.notDone()) {
//
//                while (buffer.isEmpty()) {
//                    buffer.wait();
//                }
//                buffer.read();
//
//            }
//        } catch (Exception e) {
//
//        }
//    }
//}
//
//class Buffer {
//    int count;
//    int n;
//
//    Buffer(int _n) {
//        count = 0;
//        n = _n;
//    }
//    public void write() {
//        this.count++;
//    }
//    public int read() {
//        return count;
//    }
//    public boolean notDone() {
//        if (this.count < this.n)
//            return true;
//        return false;
//    }
//    public boolean isEmpty() {
//        if (count == 0)
//            return true;
//        return false;
//    }
//}
//
//class P1 {}

class P1 {
    public static void main(String[] args) {
        try {
            A x;
            A y;
            P1 z;

            x = new A(); // O1
            y = new A(); // O2
            z = new P1(); // O3
            x.start(); // O1.start()
            x.f1 = z;
            y.start();
            y.join();
            x.join();

        } catch (Exception e) {

        }
    }
}

class A extends Thread {
    P1 f1;

    public void run() {
        try {
            A a;
            P1 b;
            a = this;
            b = new P1();
            a.f1 = b;
        } catch (Exception e) {

        }
    }
}