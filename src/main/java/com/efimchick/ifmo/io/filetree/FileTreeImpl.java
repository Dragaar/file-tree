package com.efimchick.ifmo.io.filetree;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileTreeImpl implements FileTree {
    @Override
    public Optional<String> tree(Path path) {

        if(path == null ||  !Files.exists(path)) return Optional.empty();
        StringBuilder builder = new StringBuilder();

        if(Files.isDirectory(path)){ // якщо це директорія
            walkByPath(path, builder);
           // System.out.println(builder.toString());

        } else { // якщо це файл
            walkByPath(path, builder);
            System.out.println(builder.toString());
        }


            try(BufferedWriter bfwriter = new BufferedWriter(new FileWriter("E:\\Work\\Dev\\EPAM\\Secnd\\Project\\Streams-IO\\TestFiles\\res.txt")))
            {
                bfwriter.write(builder.toString());
            } catch (IOException e){
                e.printStackTrace();
            }
        return Optional.of(builder.toString());
    }

    private void walkByPath(Path path, StringBuilder builder) {
        //https://habr.com/ru/post/437694/
        try {
            Files.walkFileTree(path, new FileVisitor<Path>() {
                int subDirectoriesIndex = 0; //рівень глубини наявної директорії

                boolean isElementLast = false;
               ArrayList<Boolean> isDirectoryLast = new ArrayList<>();
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                    isDirectoryLast.add(false);
                    //if(dir.equals(getLastFileInParentDirectory(dir))) isElementLast = true;
                    //addPathTreeToBuilder(builder, dir, subDirectoriesIndex, isElementLast);

                    if(dir.equals(getLastFileInParentDirectory(dir))){
                        isDirectoryLast.set(subDirectoriesIndex, true);
                        isElementLast = true;
                    }
                    addPathTreeToBuilder(builder, dir, subDirectoriesIndex, isDirectoryLast, isElementLast);

                    subDirectoriesIndex++;

                    isElementLast = false;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                    //System.out.println("getLastFileInParentDirectory "+ getLastFileInParentDirectory(file));
                    if(file.equals(getLastFileInParentDirectory(file))) isElementLast = true;
                    addPathTreeToBuilder(builder, file, subDirectoriesIndex, isDirectoryLast, isElementLast);


                    isElementLast = false;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    isDirectoryLast.set(subDirectoriesIndex-1, false);
                    subDirectoriesIndex--;
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int countElementsInDirectory(Path path){
        try(Stream<Path> pathStream = Files.walk(path, 1)){
            return (int)pathStream.count()-1;
        }catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
    private Path getLastFileInParentDirectory(Path path){
        try(Stream<Path> pathStream = Files.walk(path.getParent(), 1)){
            long count = countElementsInDirectory(path.getParent());
            return pathStream.skip(count).findFirst().get();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //https://www.baeldung.com/java-folder-size
    private int getRealSize(Path path){
        try{
            long r = Files.walk(path)
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
            return (int)r;
        }catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
       //інша робоча реалізація
       /* AtomicLong size = new AtomicLong(0);
        try{
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });
        }catch (IOException e) {
            e.printStackTrace();
        }
        return size.intValue();*/
    }

    private void addPathTreeToBuilder(StringBuilder builder, Path path, int subDirectoriesIndex, ArrayList<Boolean> isDirectoryLast, boolean isElementLast) throws IOException{
        if(subDirectoriesIndex>0)  {
            for(int i = 1; i < subDirectoriesIndex; i++) {
                if(isDirectoryLast.get(i)) builder.append("   ");
                else builder.append("│  ");
            }
            if(!isElementLast) builder.append("├─ ");
            else builder.append("└─  ");
        }
        builder.append(path.getFileName() + " " + getRealSize(path) + " bytes\n");
    }
}
