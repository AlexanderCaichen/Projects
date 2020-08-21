package gitlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Merge {

    public Merge(Commit a, Commit b) {

        Commit splitPoint = findSplitPoint(a,b);



    }

    /**closest split point relative to node**/
    public static Commit findSplitPoint(Commit node, Commit other) {
        Commit aTemp = node;
        Commit bTemp = other;
        if (aTemp.equals(other)) { //same commit
            return aTemp;
        }

        HashSet<String> BFamily = new HashSet<>();
        while (bTemp.getParent() != null) {
            BFamily.add(bTemp.getSha1());
            bTemp = Commit.fromFile(bTemp.getParent());
        }
        Commit last = bTemp;
        BFamily.add(last.getSha1());

        while (aTemp.getParent() != null) {
            if (BFamily.contains(aTemp.getSha1())) {
                return aTemp; //node is ancestor of other
            }
            if (aTemp.getParent2() != null) { //is a merge commit
                Commit mergeParent2 = Commit.fromFile(aTemp.getParent2());
                if (BFamily.contains(mergeParent2.getSha1())) { //if merged (something something merge branch match)
                    return mergeParent2;
                }
            }
            aTemp = Commit.fromFile(aTemp.getParent());
        }
        return last;
    }

    public static Boolean isAncestor(Commit node, Commit scale) { //is node ancestor of scale
        String Commite = node.getSha1();
        Commit bTemp = scale;
        while (bTemp.getParent() != null) {
            if (bTemp.getSha1().equals(Commite)) {
                return true;
            }
            bTemp = Commit.fromFile(bTemp.getParent());
        }
        return false;
    }

    //merges file in current head (directory) with
    public static void mergeFile(Blob current, Blob to_merge) throws IOException {
        String name = current.getFilename();
//        if (!(name.equals(to_merge.getFilename()))) {
//            System.out.println("Files not the same (name)");
//            System.exit(0);
//        }

        File temp = Utils.join(Main.GITDIR, name);
        temp.createNewFile();
        File destination = Utils.join(Main.CURRENTDIR, name);

        Utils.writeContents(temp, "<<<<<<< HEAD"+ "\n");
        byte[] save = mergebytes(Utils.readContents(temp), current.getDocumentRead()); //<<HEAD + curr doc stuff
        Utils.writeContents(temp, "======="+ "\n");
        save = mergebytes(save, Utils.readContents(temp)); //save + ===
        save = mergebytes(save, to_merge.getDocumentRead()); //save + to_merge doc stuff
        Utils.writeContents(temp, ">>>>>>>");
        save = mergebytes(save, Utils.readContents(temp));

        Utils.writeContents(temp, save); //write everything into "temp"
        //byte[] StufftoAdd = "<<<<<<< HEAD" + current.getDocumentRead() + "=======" + to_merge.getDocumentRead() + ">>>>>>>";

        temp.renameTo(destination); //does overwrite destination file if exists.
    }

    public static byte[] mergebytes(byte[] first, byte[] second) throws IOException {
        //copied from https://stackoverflow.com/questions/5513152/easy-way-to-concatenate-two-byte-arrays/23292834
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write( first );
        outputStream.write( second );

        return outputStream.toByteArray( );
    }

    /**Replaces file in directory with blob**/
    public static void replace(Blob replacement) throws IOException {
        File location = replacement.location();
//        location.createNewFile();
//        File destination = replacement.location();
        if (!(location.isFile())) {
            location.createNewFile();
        }

        Utils.writeContents(location, replacement.getDocumentRead());

        //location.renameTo(destination); //writes if it doesn't exist. writes if it does exist.

        Main.addFileToStaging(location);
    }

}
