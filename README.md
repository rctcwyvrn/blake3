BLAKE3 in Java
---
A completely unoptimized translation of the blake3 reference implementation from rust to java.
Useful for if you want a single file implementation of blake3 with no dependencies and don't care about speed.

If what you are java bindings for blake3 that are fully optimized try: https://github.com/sken77/BLAKE3jni
```java
        // Hashing strings
        Blake3 hasher = new Blake3();
        hasher.update("This is a string");
        String hexhash = hasher.hexdigest();
```
```java
        // Hashing files
        String filename = "src/com/github/rctcwyvrn/blake3java/Blake3.java";
        Blake3 fileHasher = new Blake3();
        fileHasher.updateFile(filename);
        String filehash = fileHasher.hexdigest();
```