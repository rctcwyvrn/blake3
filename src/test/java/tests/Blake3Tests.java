package tests;

import io.github.rctcwyvrn.blake3.Blake3;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class Blake3Tests {
    private static final byte[] testBytes = "This is a string".getBytes(StandardCharsets.UTF_8);
    private static final byte[] testKeyedHashBytes = new byte[] {
            0x41,0x41,0x41,0x41,0x41,0x41,0x41,0x41,
            0x41,0x41,0x41,0x41,0x41,0x41,0x41,0x41,
            0x41,0x41,0x41,0x41,0x41,0x41,0x41,0x41,
            0x41,0x41,0x41,0x41,0x41,0x41,0x41,0x41
    };

    @Test
    public void basicHash(){
        Blake3 hasher = new Blake3();
        hasher.update(testBytes);
        assertEquals("718b749f12a61257438b2ea6643555fd995001c9d9ff84764f93f82610a780f2", hasher.hexdigest());
    }

    @Test
    public void testLongerHash(){
        Blake3 hasher = new Blake3();
        hasher.update(testBytes);
        assertEquals( "718b749f12a61257438b2ea6643555fd995001c9d9ff84764f93f82610a780f243a9903464658159cf8b216e79006e12ef3568851423fa7c97002cbb9ca4dc44b4185bb3c6d18cdd1a991c2416f5e929810290b24bf24ba6262012684b6a0c4e096f55e8b0b4353c7b04a1141d25afd71fffae1304a5abf0c44150df8b8d4017",
                hasher.hexdigest(128));
    }

    @Test
    public void testShorterHash(){
        Blake3 hasher = new Blake3();
        hasher.update(testBytes);
        assertEquals("718b749f12a61257438b2ea6643555fd",hasher.hexdigest(16));
    }

    @Test
    public void testRawByteHash(){
        Blake3 hasher = new Blake3();
        hasher.update(testBytes);
        byte[] digest = hasher.digest();
        assertTrue(Arrays.equals(digest, new byte[]{
                113, -117, 116, -97, 18, -90, 18, 87, 67, -117, 46, -90, 100, 53, 85, -3, -103, 80, 1, -55, -39, -1,
                -124, 118, 79, -109, -8, 38, 16, -89, -128, -14
        }));
    }

    @Test
    public void testFileHash(){
        try {
            Blake3 hasher = new Blake3();
            hasher.update(new File("LICENSE"));
            assertEquals("381f3baeddb0ce5202ac9528ecc787c249901e74528ba2cbc5546567a2e0bd33", hasher.hexdigest());
        } catch (Exception e){
            fail("Exception thrown");
        }
    }

    @Test
    public void testKeyedFileHash(){
        try {
            Blake3 hasher = new Blake3(testKeyedHashBytes);
            hasher.update(new File("LICENSE"));
            assertEquals("e0d0ef068716e24b845abd9e7aba97d28d9c1551559cb53899ce50a2dab982cd", hasher.hexdigest());
        } catch (Exception e){
            fail("Exception thrown");
        }
    }

    @Test
    public void testKDFHash(){
        Blake3 hasher = new Blake3("meowmeowverysecuremeowmeow");
        hasher.update(testBytes);
        assertEquals("348de7e5f8f804216998120d1d05c6d233d250bdf40220dbf02395c1f89a73f7", hasher.hexdigest());
    }

    @Test
    public void testBigFileHash(){
        try {
            Blake3 hasher = new Blake3();
            hasher.update(new File("src/test/resources/bigFileTest"));
            assertEquals("3f3bc092223538bbc4026c8e3de04513b6de622181963934d11b8c1e876fe194", hasher.hexdigest());
        } catch (Exception e){
            fail("Exception thrown");
        }
    }
}
