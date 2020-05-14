BLAKE3 in Java
---
An unoptimized translation of the blake3 reference implementation from rust to java.
### Examples
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

If what you want are java bindings for the fully optimized blake3, try: https://github.com/sken77/BLAKE3jni
