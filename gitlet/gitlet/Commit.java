package gitlet;
import com.sun.source.doctree.CommentTree;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Commit implements Serializable {

    //need clarification on this
    private static final long serialVersionUID = -8419229610344905667L;
    private String date;
    private String message;
    private String parent2 = null;

    //change parent to be a list or array
    /** SHA1 of parent commit*/
    private String parent;
    private String sha1;
    private List<String> commitBlobs = Utils.plainFilenamesIn(Main.STAGEDDIR);
    private HashMap<String, String> name_list;

    //for split/merge purposes
    private boolean split = false;


    public Commit(String message, String sha1Parent, String date) throws IOException {
        if (date == null) {
            this.date = getCurrentTime();
        }
        else { this.date = date; }
        this.message = message;
        this.parent = sha1Parent;

        if (parent == null) {
            this.name_list = new HashMap<>();
        }
        else {
            File head_commit = Utils.join(Main.COMMITDIR, Main.getHeadCommitID());
            HashMap<String, String> prevDict = Utils.readObject(head_commit, Commit.class).getDictionary();
            prevDict.putAll(Blob.file_dict); //over-rides any previous blob with same file name +add new ones
            for (String i : Blob.removed.keySet()) {
                if (prevDict.keySet().contains(i)) {
                    prevDict.remove(i);
                }
            }
            this.name_list = prevDict;
        }

        this.sha1 = Utils.sha1(Utils.serialize(this));
        writeCommitToFile();
        Main.setBranchPointer(sha1, Main.getHeadBranchName());

    }

    public Commit(String message, String sha1Parent, String date, String parent2) throws IOException {
        if (date == null) {
            this.date = getCurrentTime();
        }
        else { this.date = date; }
        this.message = message;
        this.parent = sha1Parent;
        this.parent2 = parent2;

        if (parent == null) {
            this.name_list = new HashMap<>();
        }
        else {
            File head_commit = Utils.join(Main.COMMITDIR, Main.getHeadCommitID());
            HashMap<String, String> prevDict = Utils.readObject(head_commit, Commit.class).getDictionary();
            prevDict.putAll(Blob.file_dict); //over-rides any previous blob with same file name +add new ones
            for (String i : Blob.removed.keySet()) {
                if (prevDict.keySet().contains(i)) {
                    prevDict.remove(i);
                }
            }
            this.name_list = prevDict;
        }

        this.sha1 = Utils.sha1(Utils.serialize(this));
        writeCommitToFile();
        Main.setBranchPointer(sha1, Main.getHeadBranchName());

    }

    private void writeCommitToFile() throws IOException {
        File commit = Utils.join(Main.COMMITDIR,sha1);
        if (!(commit).exists()) {
            commit.createNewFile();
            Utils.writeObject(commit, this);
        } else {
            return;
        }
    }

    public static Commit fromFile(String sha) {
        File commit = Utils.join(Main.COMMITDIR,sha);
        Commit a = Utils.readObject(commit, Commit.class);
        return a;

    }

    public static List<String> getAllCommitNames() {
        List<String> commitNames = Utils.plainFilenamesIn(Main.COMMITDIR);
        return commitNames;
    }


    public static String getCommitIDFromAbreviated(String abreviatedCommitID) {
        if (!(Utils.join(Main.COMMITDIR, abreviatedCommitID).exists())) {
            List<String> allCommits = Utils.plainFilenamesIn(Main.COMMITDIR);
            //check if commitID is substring of a commit
            for (int i = 0; i < allCommits.size(); i++) {
                if (allCommits.get(i).contains(abreviatedCommitID)) {
                    abreviatedCommitID = allCommits.get(i);
                    break;
                }

                if (i == allCommits.size() - 1) {
                    System.out.println("No commit with that id exists.");
                    System.exit(0);
                }
            }
        }

        return abreviatedCommitID;
    }


    public static String getCurrentTime() {
//        borrowed from http://tutorials.jenkov.com/java-internationalization/simpledateformat.html
        String pattern = "EEE MMM dd HH:mm:ss yyyy";
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(pattern, new Locale("en", "USA"));

        String date = simpleDateFormat.format(new Date());
        return (date);

    }

    public static void revertAllFiles(Commit commit) {
        HashMap<String, String> commitBlobDict = commit.getDictionary();

        Set<String> fileNamesBranchtoCheckout = commitBlobDict.keySet();
        for (String name: fileNamesBranchtoCheckout) {

            Blob readBlob = Blob.getBlob(Utils.join(Main.BLOBDIR, commitBlobDict.get(name)));
            byte[] readDocumentSnapshot = readBlob.getDocumentRead();
            File originalFileLocation = Utils.join(Main.CURRENTDIR, name);
            if (!(originalFileLocation.isFile())) {
                continue;
            }
            Utils.writeContents(originalFileLocation, readDocumentSnapshot);
        }
    }

    /** checks the relative location of Commit b to current head. Possible results are:
     * "upstream", "downstream", "different split", or "same
     * @param a
     * @param b
     * @return
     */
    public static String checkRelativeLocation(Commit b) {

        Commit a = Commit.fromFile(Main.getHeadCommitID());

        if (a.sha1.equals(b.sha1)) {
            return "same";
        }

        if (a.getParent() == null) {
            return "upstream";
        }
        Commit tempA = Commit.fromFile(a.getParent());

        while (!(tempA.getParent() == null)) {
            if (tempA.sha1.equals(b.sha1)){
                return "downstream";
            }
            if (tempA.getParent() == null) {
                break;
            }
            tempA = Commit.fromFile(tempA.getParent());
        }
        Commit tempB = Commit.fromFile(b.getParent());

        while (!(tempB.getParent() == null)) {
            if (tempB.sha1.equals(a.sha1)){
                return "upstream";
            }

            if (tempB.getParent() == null) {
                break;
            }
            tempB = Commit.fromFile(tempB.getParent());
        }

        return "different split";



    }

    public String getParent() {return parent; }

    public String getMessage() {return message; }

    public String getDate() {return date; }

    public void setParent(String parentSHA) { parent = parentSHA; }

    public String getSha1() {return sha1; }

    public HashMap getDictionary() {return name_list;}

    public List<String> getCommitBlobs() {return commitBlobs; }

    public String getParent2() {return parent2; }

    public void setAsSplit() {this.split = true; }


    public Commit findSplitPoint(Commit b) {
        Commit aTemp = this;
        Commit bTemp = b;
        HashSet<Commit> allCommits = new HashSet<>();
        while (!(aTemp.getParent() == null)) {
            allCommits.add(aTemp);
            aTemp = Commit.fromFile(aTemp.getParent());
        }

        while (!(bTemp.getParent() == null)) {
            if (allCommits.contains(bTemp)) {
                Commit splitPoint = bTemp;
                return splitPoint;
            }

            bTemp = Commit.fromFile(bTemp.getParent());
        }
        return null;
    }

}
