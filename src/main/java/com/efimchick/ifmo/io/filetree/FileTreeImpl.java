package com.efimchick.ifmo.io.filetree;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileTreeImpl implements FileTree {

    int subDirectoriesIndex = 0; //рівень глубини наявної директорії

    boolean isLocalElementLast = false;
    boolean rootDirectory = true;
    ArrayList<Boolean> isGlobalElementLast = new ArrayList<>();


    @Override
    public Optional<String> tree(Path path) {

        if(path == null ||  !Files.exists(path)) return Optional.empty();
        StringBuilder builder = new StringBuilder();

        if(Files.isDirectory(path)){ // якщо це директорія
            isGlobalElementLast.add(false); //додатковий елемент для кореня
            walkByPath(path, builder);

        } else { // якщо це файл
            walkByPath(path, builder);
            System.out.println(builder.toString());
        }

        // Виведення результату в файл для перевірки
          /*  try(BufferedWriter bfwriter = new BufferedWriter(new FileWriter("E:\\Work\\Dev\\EPAM\\Secnd\\Project\\Streams-IO\\TestFiles\\res.txt")))
            {
                bfwriter.write(builder.toString());
            } catch (IOException e){
                e.printStackTrace();
            }*/
        return Optional.of(builder.toString());
    }

    private void walkByPath(Path path, StringBuilder builder) {

        try {
            isGlobalElementLast.add(false);

            List<Path> files;
            List<Path> directories;
            try (Stream<Path> walk = Files.walk(path, 1)) {
                files = walk.filter(Files::isRegularFile)
                        .sorted()
                        .collect(Collectors.toList());
            }
            try (Stream<Path> walk = Files.walk(path, 1)) {
                directories = walk.filter(Files::isDirectory)
                        .sorted()
                        .collect(Collectors.toList());
            }

            //System.out.println("directories " + directories);

            for(int i = 0; i <= directories.size()-1; i++){
                if(i == directories.size()-1){
                    if(files.isEmpty()){
                        isGlobalElementLast.set(subDirectoriesIndex, true);
                        isLocalElementLast = true;}
                }
                if(rootDirectory){
                    addPathTreeToBuilder(builder, directories.get(i), subDirectoriesIndex, isGlobalElementLast, isLocalElementLast);
                    subDirectoriesIndex++;
                    rootDirectory = false;
                }
                if(i>0) {
                    addPathTreeToBuilder(builder, directories.get(i), subDirectoriesIndex, isGlobalElementLast, isLocalElementLast);
                    isLocalElementLast = false;
                    subDirectoriesIndex++;
                    walkByPath(directories.get(i), builder);
                }
            }
            for(int i = 0; i <= files.size()-1; i++){
                if(i == files.size()-1){
                        //isGlobalElementLast.set(subDirectoriesIndex-1, true);
                        isLocalElementLast = true;
                }
                addPathTreeToBuilder(builder, files.get(i), subDirectoriesIndex, isGlobalElementLast, isLocalElementLast);
                isLocalElementLast = false;
            }
            if(subDirectoriesIndex == 0)  isGlobalElementLast.set(subDirectoriesIndex, false);
            else isGlobalElementLast.set(subDirectoriesIndex-1, false);
            subDirectoriesIndex--;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*  private void walkByPath2(Path path, StringBuilder builder) {
        //https://habr.com/ru/post/437694/
        try {
            Files.walkFileTree(path, new FileVisitor<Path>() {

                int subDirectoriesIndex = 0; //рівень глубини наявної директорії

                boolean isElementLast = false;
               ArrayList<Boolean> isDirectoryLast = new ArrayList<>();

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                    isDirectoryLast.add(false);

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
    }*/
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
            else builder.append("└─ ");
        }
        builder.append(path.getFileName() + " " + getRealSize(path) + " bytes\n");
    }
}
