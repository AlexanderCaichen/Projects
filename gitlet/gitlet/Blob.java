package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;


public class Blob implements Serializable {

    //need clarification on this
    private static final long serialVersionUID = -8419229610344905667L;

    private byte[] documentRead;
    private String filename;
    private File location;
    private String blobSha1;


    //Because Tests aren't friendly with static stuff. (name of file, SHA1 of file)
    public static HashMap<String, String> file_dict = Utils.readObject(Main.STAGED, HashMap.class);
    public static HashMap<String, File> removed = Utils.readObject(Main.REMOVED, HashMap.class);


    public Blob(File file) throws IOException {
        documentRead = Utils.readContents(file);
        filename = file.getName();
        location = file;
        blobSha1 = Utils.sha1(Utils.serialize(documentRead));
    }

    public void writeBlobToStaging() throws IOException {

//        File oldBlob = Utils.join(Main.BLOBDIR, blobSha1);
//        if (oldBlob.exists()) { //if blob is already there, don't do anything.
//            return;
//        } else { //PROBLEM: can be in staged (added before) but then rm in current commit. Added again now.
        File toBeStaged = Utils.join(Main.STAGEDDIR, blobSha1);
        toBeStaged.createNewFile();
        Utils.writeObject(toBeStaged, this);
        //System.out.println("adding success");
        file_dict.put(filename, blobSha1);
        Utils.writeObject(Main.STAGED, file_dict);
    }

    public static List<String> getAllStagedBlobsNames() {
        List<String> stagedBlobNames = Utils.plainFilenamesIn(Main.STAGEDDIR);
        return stagedBlobNames;
    }

    /** checks if there is a specific blob staged, and if so, removes it.
     * To be used when adding a file to staging, as to not have multiple staged
     * blobs of the same file
     * @param file is file to check. The actual file (to be added to staging area)
     * @return true if existed, else false
     */
    public static void checkAndRemoveIfFileAlreadyStaged(File file) throws IOException {
        String name = file.getName();
        if (file_dict.containsKey(name)) { //file staged
            String code = file_dict.get(name);
            Utils.join(Main.STAGEDDIR, code).delete();
            file_dict.remove(name);
            Utils.writeObject(Main.STAGED, file_dict);
        }
    /*List<String> blobNames = getAllStagedBlobsNames();
        for (int i = 0; i < blobNames.size(); i++) {
            if (getBlob(Utils.join(Main.STAGEDDIR,blobNames.get(i))).getFilename().equals(name)) {
                Utils.join(Main.STAGEDDIR, blobNames.get(i)).delete();
                break;
            }
        }*/
    }

    /** moves staged serialized files to general blob population. To be used once Committed */
    public static void moveStagedFileNames() {
        List<String> blobs = Utils.plainFilenamesIn(Main.STAGEDDIR);
        for (int i = 0; i < blobs.size(); i++){
            File newFile = Utils.join(Main.BLOBDIR, blobs.get(i));
            if (newFile.isFile()) { //if the file already exists in Blob directory from some previous add
                Utils.join(Main.STAGEDDIR, blobs.get(i)).delete();
            }
            else {
                Utils.join(Main.STAGEDDIR, blobs.get(i)).renameTo(newFile);
            }
        }
    }

    public static Blob getBlob(File blobFile) {
        return Utils.readObject(blobFile, Blob.class);
    }

    public String getFilename() {
        return filename;
    }

    public static List<String> getStagedBlobs() {
        return Utils.plainFilenamesIn(Main.STAGEDDIR);
    }

    public byte[] getDocumentRead() {
        return documentRead;
    }

    public File location() {
        return location;
    }

    public String Sha1() {
        return blobSha1;
    }

    /**does blob file exist in directory**/
    public boolean exists() {
        return this.location.isFile();
    }

    /**is blob file different from in directory**/
    public boolean changed() throws IOException {
        if (exists()) {
            Blob nowBlob = new Blob(this.location);
            if (this.blobSha1.equals(nowBlob.blobSha1)) {
                return false; //not changed
            }
            else {
                return true;
            }
        }
        else {
            return true;
        }
    }

    /**is blob file different from ones in most recent commit, assumes file is being tracked.**/
    public boolean changed2() throws IOException {
        if (exists()) {
            File head_commit = Utils.join(Main.COMMITDIR, Main.getHeadCommitID());
            Commit unzipped = Utils.readObject(head_commit, Commit.class);
            String prevCode = (String) unzipped.getDictionary().get(this.filename);
            return prevCode.equals(this.blobSha1);
        }
        else {
            return true;
        }
    }

    /**Only checks if file is being tracked in most recent commit, not if it exists or not**/
    public static boolean tracked(File file) {
        File head_commit = Utils.join(Main.COMMITDIR, Main.getHeadCommitID());
        Commit unzipped = Utils.readObject(head_commit, Commit.class);
        //"tracked by current/head commit"
        return (unzipped.getDictionary().containsKey(file.getName()));
    }

    public static void clearStagingArea() {
        List<String> allStaged = Utils.plainFilenamesIn(Main.STAGEDDIR);
        for (String staged: allStaged) {
            Utils.join(Main.STAGEDDIR, staged).delete();
        }
        Blob.file_dict = new HashMap<>();
        Utils.writeObject(Main.STAGED, Blob.file_dict);
        Blob.removed = new HashMap<>();
        Utils.writeObject(Main.REMOVED, Blob.removed);
    }
}
