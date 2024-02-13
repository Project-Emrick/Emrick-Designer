import java.io.File;

class UnzipTest {

    @org.junit.jupiter.api.Test
    void unzip() {
        File fs = new File("test" + File.separator + "Purdue23-1-1aint_no_mountain_high_enough.3dz");
        File fd = new File("test" + File.separator + "Purdue23-1-1aint_no_mountain_high_enough");
        System.out.println(fs.getAbsolutePath());
        Unzip.unzip(fs.getAbsolutePath(), fd.getAbsolutePath());
    }
}