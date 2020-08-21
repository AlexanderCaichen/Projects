package gitlet;
import java.io.IOException;
import java.io.File;
import java.util.*;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */

    static File CURRENTDIR = new File(System.getProperty("user.dir"));
    static File GITDIR = Utils.join(CURRENTDIR, ".gitlet");
    static File COMMITDIR = Utils.join(GITDIR, "commits");
    static File BLOBDIR = Utils.join(GITDIR, "blobs");
    static File BRANCHESDIR = Utils.join(GITDIR, "branches");
    static File MASTERPOINTERFILE = Utils.join(BRANCHESDIR, "master");
    static File STAGEDDIR = Utils.join(BLOBDIR, "staged");
    static File HEADPOINTERFILE = Utils.join(GITDIR, "head");

    static File STAGED = Utils.join(BLOBDIR, "file_dict");
    static File REMOVED = Utils.join(BLOBDIR, "removed");

    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        switch (args[0]) {
            case "init":
                CheckArgLength(args, 1);
                setupGit();
                return;

            case "add":
                exitIfNotInstantiatedGit();
                CheckArgLength(args, 2);
                File fileToAdd = Utils.join(Main.CURRENTDIR, args[1]);
                addFileToStaging(fileToAdd);
                return;

            case "commit":
                exitIfNotInstantiatedGit();
                CheckArgLength(args, 2);
                String message = args[1];
                if (message.length() == 0) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                if (Blob.getStagedBlobs().isEmpty() && Blob.file_dict.isEmpty() && Blob.removed.isEmpty()) { //maybe "is empty" is a problem?
                    System.out.println("No changes added to the commit.");
                    System.exit(0);
                }
                Commit newCommit = new Commit(message, Main.getHeadCommitID(), null);
                Blob.moveStagedFileNames();

                HashMap<String, String> StagedDict = new HashMap<>();
                Utils.writeObject(STAGED, StagedDict);
                Blob.file_dict = new HashMap<>();

                HashMap<String, File> removedDict = new HashMap<>();
                Utils.writeObject(REMOVED, removedDict);
                Blob.file_dict = new HashMap<>();
                return;

            case "rm":
                exitIfNotInstantiatedGit();
                CheckArgLength(args, 2);
                File fileToRemove = Utils.join(Main.CURRENTDIR, args[1]);
                removeFilefromStaging(fileToRemove);
                return;

            case "log":
                exitIfNotInstantiatedGit();
                CheckArgLength(args, 1);
                log();
                return;

            case "global-log":
                exitIfNotInstantiatedGit();
                CheckArgLength(args, 1);
                globlog();
                return;

            case "find":
                exitIfNotInstantiatedGit();

                if (args[1].charAt(0) == '"') {
                    args[1] = args[1].substring(1, args[1].length() -1);
                }
                findCommit(args[1]);
                return;

            case "status":
                exitIfNotInstantiatedGit();
                printstatus();
                return;

            case "checkout":
                exitIfNotInstantiatedGit();
                //checkout [branch name]
                if (args.length == 2) {
                    checkout(null, null, args[1]);
                    return;
                }
                //checkout -- [file name]
                if (args.length == 3) {
                    if (!(args[1].equals("--"))) {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    String currentCommit = getHeadCommitID();
                    checkout(currentCommit, args[2], null);
                    return;
                }
                //checkout [commit id] -- [file name]
                if (args.length == 4) {
                    if (!(args[2].equals("--"))) {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    checkout(args[1], args[3], null);
                    return;
                } else {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }

            case "branch":
                CheckArgLength(args, 2);
                exitIfNotInstantiatedGit();
                newBranch(args[1]);
                return;

            case "rm-branch":
                exitIfNotInstantiatedGit();
                CheckArgLength(args, 2);
                removeBranch(args[1]);
                return;

            case "reset":
                exitIfNotInstantiatedGit();
                CheckArgLength(args, 2);
                reset(args[1]);
                return;

            case "merge":
                exitIfNotInstantiatedGit();
                CheckArgLength(args, 2);
                merge(args[1]);
                return;

            case "rebase":
                exitIfNotInstantiatedGit();

        }
        System.out.println("No command with that name exists.");
        System.exit(0);
    }

    /**Checks if argument length is correct (based on input)**/
    public static void CheckArgLength(String[] args, int lengthy) {
        if (args.length != lengthy) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    /** Creates necessary directories and files: (.git directory, commit directory within .git,
     * blob directory within .git, HEADPOINTERFILE within .git that contains SHA1 value of current
     * head, MASTERPOINTERFILE within .git that contains SHA1 value of current master, and creates
     * an initial commit instance, storing it in ~/.git/commits/
     * @throws IOException
     */
    public static void setupGit() throws IOException {
        if (GITDIR.exists()) {
            System.out.println(
                    "A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        GITDIR.mkdir();
        BLOBDIR.mkdir();
        COMMITDIR.mkdir();
        STAGEDDIR.mkdir();
        BRANCHESDIR.mkdir();
        HEADPOINTERFILE.createNewFile();
        MASTERPOINTERFILE.createNewFile();

        STAGED.createNewFile();
        REMOVED.createNewFile();

        setHeadPointer("master");

        HashMap<String, String> StagedDict = new HashMap<>();
        Utils.writeObject(STAGED, StagedDict);

        HashMap<String, File> removedDict = new HashMap<>();
        Utils.writeObject(REMOVED, removedDict);

        Commit initialCommit = new Commit("initial commit", null, "Wed Dec 31 16:00:00 1969");
    }

    private static void exitIfNotInstantiatedGit() {
        if (!(Main.GITDIR.exists())) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    public static void reset(String commitID) throws IOException {

        List<String> Committe = Commit.getAllCommitNames();
        if (!(Committe.contains(commitID))) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        //error cases handled here//
        String fullCommitID = Commit.getCommitIDFromAbreviated(commitID);
        Commit commit = Commit.fromFile(fullCommitID);
        HashMap<String,String> replaceFileDict = commit.getDictionary();

//        File commitLocation = Utils.join(Main.COMMITDIR, commitID);
        ArrayList<String> fileNamesCommit = new ArrayList<>(replaceFileDict.keySet());


        //perform this check before anything else.
        for (String i : fileNamesCommit) {
            File blobs = Utils.join(BLOBDIR, replaceFileDict.get(i));
            Blob Bloby = Utils.readObject(blobs, Blob.class);
            File location = Bloby.location(); //Utils.join(CURRENTDIR, filesToReplace.get(i));

            if (!(Blob.tracked(location)) && location.isFile()) {
                Blob temp = new Blob(location);
                if (!(temp.Sha1().equals(Bloby.Sha1()))) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        for (String a: fileNamesCommit) {
            checkout(fullCommitID, a, null);
        }

        //HashMap<String, String> currentCommitBlobDict = currentCommit.getDictionary();
        //Set<String> currentCommitFileNames = currentCommitBlobDict.keySet();

        ////Checking if a working file is untracked in the current branch and would be
        // overwritten by the checkout

//        for (String workingFile: Utils.plainFilenamesIn(CURRENTDIR)) {
//            if (!((currentCommit.getDictionary().containsKey(workingFile))) &&
//                    fileNamesCommit.contains(workingFile)) {
//                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
//                System.exit(0);
//            }
//        }

        //finished checking
        setBranchPointer(commitID, getHeadBranchName());
        Blob.clearStagingArea();
    }

    public static void removeBranch(String branchName) {
        if (!(Utils.plainFilenamesIn(BRANCHESDIR).contains(branchName))) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        if (getHeadBranchName().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        Utils.join(BRANCHESDIR, branchName).delete();
    }

    /** Prints out the ids of all commits that have the given commit message, one per line.
     * If there are multiple such commits, it prints the ids out on separate lines.
     * The commit message is a single operand; to indicate a multiword message,
     * put the operand in quotation marks, as for the commit command below.
     */
    public static void findCommit(String commitMessage) {
        List<String> allCommits = Commit.getAllCommitNames();
        int numberFound = 0;
        String trimmedCommitMessage = commitMessage.trim(); //???????
        for (String a: allCommits) {
            File commitFile = Utils.join(Main.COMMITDIR, a);
            if(Commit.fromFile(a).getMessage().equals(trimmedCommitMessage)) {
                System.out.println(a);
                numberFound ++;
            }
        }
        if (numberFound == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    public static void newBranch(String name) throws IOException {
        if (Utils.join(BRANCHESDIR, name).isFile()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        File newBranch = Utils.join(BRANCHESDIR, name);
        Utils.writeContents(newBranch, getHeadCommitID());
    }

    public static void addFileToStaging(File name) throws IOException {
        if (!(name.isFile())) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        Blob newBlob = new Blob(name);
        String FileName = name.getName();

        //If the current working version of the file is identical to the version in the
        //current commit, do not stage it to be added
        File head_commit = Utils.join(Main.COMMITDIR, Main.getHeadCommitID());
        Commit unzipped = Utils.readObject(head_commit, Commit.class);
        if (unzipped.getDictionary().keySet().contains(FileName)) { //"tracked by current/head commit"
            String prevVersion = (String) unzipped.getDictionary().get(FileName);
            if (prevVersion.equals(newBlob.Sha1())) { //same version file (same contents)
                //remove it from the staging area if it is already there
                if (Blob.file_dict.containsKey(FileName)) {
                    Utils.join(Main.STAGEDDIR, Blob.file_dict.get(FileName)).delete();
                    Blob.file_dict.remove(FileName);
                    Utils.writeObject(STAGED, Blob.file_dict);
                }
                if (Blob.removed.containsKey(FileName)) {
                    Blob.removed.remove(FileName);
                    Utils.writeObject(REMOVED, Blob.removed);
                }
                return;
            }
        }

        Blob.checkAndRemoveIfFileAlreadyStaged(name);

        newBlob.writeBlobToStaging();
        //System.out.println(Blob.file_dict);
    }


    /**If the file is tracked in the current commit, stage it for removal and remove the file from the
     * working directory if the user has not already done so (do not remove it unless it is tracked in the
     * current commit).**/
    public static void removeFilefromStaging(File name) throws IOException {

        Boolean staged = Blob.file_dict.containsKey(name.getName());
        Boolean tracked = Blob.tracked(name);

        //If the file is neither staged nor tracked by the head commit, print the error message
        if (!(staged) && !(tracked)) { //if staged==false and tracked == false
            System.out.println("No reason to remove the file.");
            return;
        }

        //"Unstage the file if it is currently staged for addition."
        Blob.checkAndRemoveIfFileAlreadyStaged(name);

        // (do not remove it unless it is tracked in the current commit)
        if (tracked) { //"tracked by current/head commit"
            Blob.removed.put(name.getName(), name);
            Utils.writeObject(REMOVED, Blob.removed);
            if (name.isFile()) { //if the user has not already done so, stage it for removal and remove the file from the working directory
                name.delete();
            }//If already deleted, already staged for deletion
        }
    }


    /** Used only for Unit Testing */
    public static boolean deleteDirectory(File dir) {
//        borrowed from https://javarevisited.blogspot.com/2015/03/
//        how-to-delete-directory-in-java-with-files.html#:~:text=
//        Deleting%20an%20empty%20directory%20is,
//        contains%20files%20or%20sub%20folders.
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDirectory(children[i]);
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**makes head file in ".gitlet" (what is current HEAD commit) contain "branchName"**/
    public static void setHeadPointer(String branchName) {
        Utils.writeContents(Main.HEADPOINTERFILE, branchName);
    }

    public static void setMasterPointer(String sha1) throws IOException {
        setBranchPointer(sha1, "master");

    }

    public static String getHeadBranchName() {
        return Utils.readContentsAsString(HEADPOINTERFILE);
    }

    public static String getHeadCommitID() {
        String branchName = Utils.readContentsAsString(HEADPOINTERFILE);
        return getBranchPointer(branchName);
    }

    public static String getMasterPointer() {
        return getBranchPointer("master");
    }

    /** will modify the contents of branch specified or create a new document
     * with the sha1 hash if it doesn't already exist
     * @param commitSha1
     * @param branchName <<Makes file with this name in "branches" folder contain "commitSha1"
     */
    public static void setBranchPointer(String commitSha1, String branchName) throws IOException {
        Utils.writeContents(Utils.join(BRANCHESDIR, branchName), commitSha1);
    }

    /** returns commit id associated with the last commit on that branch (name of branch, not ID) */
    public static String getBranchPointer(String branchName) {

        List<String> allBranches = getAllBranches();

        if(!((Utils.join(BRANCHESDIR, branchName)).isFile())) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        return Utils.readContentsAsString(Utils.join(BRANCHESDIR, branchName));
    }

    /**get list of all branch names**/
    public static List<String> getAllBranches() {
        return Utils.plainFilenamesIn(BRANCHESDIR);
    }



    public static void log() {

        String head = getHeadCommitID();
        File commitLocation = Utils.join(Main.COMMITDIR, head);
        Commit currCommit = Utils.readObject(commitLocation, Commit.class);

        while (currCommit.getParent() != null) {
            System.out.println("===");
            System.out.println("commit " + currCommit.getSha1());
            if (currCommit.getParent2() != null) {
                System.out.print("Merge: ");
                for (int i = 0; i<7; i++) {
                    System.out.print(currCommit.getParent().charAt(i));
                }
                System.out.print(" ");
                for (int i = 0; i<7; i++) {
                    System.out.print(currCommit.getParent2().charAt(i));
                }
                System.out.println();
            }
            System.out.println("Date: " + currCommit.getDate() + " -0800");
            System.out.println(currCommit.getMessage());
            System.out.println();
            currCommit = Commit.fromFile(currCommit.getParent());
        }

        System.out.println("===");
        System.out.println("commit " + currCommit.getSha1());
        System.out.println("Date: " + currCommit.getDate() + " -0800");
        System.out.println(currCommit.getMessage());
    }

    public static void globlog() {
        ArrayList<String> AllCommits = new ArrayList(Utils.plainFilenamesIn(Main.COMMITDIR));

        for (int i = 0; i<(AllCommits.size()-1); i++) {
            File committee = Utils.join(Main.COMMITDIR, AllCommits.get(i));
            Commit unzipped = Utils.readObject(committee, Commit.class);
            System.out.println("===");
            System.out.println("commit " + unzipped.getSha1());
            if (unzipped.getParent2() != null) {
                System.out.print("Merge: ");
                for (int a = 0; i<7; i++) {
                    System.out.print(unzipped.getParent().charAt(a));
                }
                System.out.print(" ");
                for (int a = 0; i<7; i++) {
                    System.out.print(unzipped.getParent2().charAt(a));
                }
                System.out.println();
            }
            System.out.println("Date: " + unzipped.getDate() + " -0800");
            System.out.println(unzipped.getMessage());
            System.out.println();
        }

        File committee = Utils.join(Main.COMMITDIR, AllCommits.get(AllCommits.size()-1));
        Commit unzipped = Utils.readObject(committee, Commit.class);
        System.out.println("===");
        System.out.println("commit " + unzipped.getSha1());
        System.out.println("Date: " + unzipped.getDate() + " -0800");
        System.out.println(unzipped.getMessage());
    }

    public static void printstatus() throws IOException {
        //########### Note: MUST BE ALPHABETICAL ORDER #####################
        //Don't worry about subdirectories.
        System.out.println("=== Branches ===");
        List<String> branches = Main.getAllBranches();
        //Sort is from https://howtodoinjava.com/sort/sort-arraylist-strings-integers/
        branches.sort(Comparator.comparing( String::toString ));
        for (int i = 0; i< branches.size(); i++){
            if (branches.get(i).equals(getHeadBranchName())) {
                System.out.println("*"+branches.get(i));
                continue;
            }
            System.out.println(branches.get(i));
        }
        System.out.println();

        //##################################################################
        System.out.println("=== Staged Files ===");
        ArrayList<String> staged = new ArrayList<>(Blob.file_dict.keySet());
        //System.out.println(Blob.file_dict);
        Collections.sort(staged);
        for (int i = 0; i<staged.size(); i++) {
            System.out.println(staged.get(i));
        }
        System.out.println();

        //##################################################################
        System.out.println("=== Removed Files ===");
        ArrayList<String> removed = new ArrayList<>(Blob.removed.keySet());
        //System.out.println(Blob.removed);
        Collections.sort(removed);
        for (int i = 0; i<removed.size(); i++) {
            System.out.println(removed.get(i));
        }
        System.out.println();

        //##################################################################
        System.out.println("=== Modifications Not Staged For Commit ===");
//        ArrayList<String> NotStaged = new ArrayList<>();
//        //ArrayList<String> staged = new ArrayList<>(Blob.file_dict.keySet()); (line 434)
//
//        File head_commit = Utils.join(Main.COMMITDIR, Main.getHeadCommitID());
//        Commit unzipped = Utils.readObject(head_commit, Commit.class);
//        ArrayList<String> tracked = new ArrayList(unzipped.getDictionary().keySet()); //commit's keys
//
//        //***Tracked in the current commit*** BUT
//        for (int i = 0; i < tracked.size(); i++) {
//            String key = tracked.get(i); //name of file tracked
//            File a = Utils.join(Main.CURRENTDIR, key); //File in directory (at least location)
//
//            //changed in the working directory, but not staged
//            if (a.isFile()) {
//                Blob b = new Blob(a); //(temp) blob of "file"
//                String prevCode = (String) unzipped.getDictionary().get(key); //SHA1 of prev file
//                if (!(b.Sha1().equals(prevCode)) && !(staged.contains(a.getName()))) {
//                    NotStaged.add(a.getName() + " (modified)");
//                    //System.out.println(NotStaged);
//                }
//            }
//            //Not staged for removal and deleted from the working directory.
//            if (!(a.isFile()) && !(removed.contains(key))) {
//                NotStaged.add(key + " (deleted)");
//            }
//        }
//
//        //***Staged for addition*** BUT
//        //ArrayList<String> staged = new ArrayList<>(Blob.file_dict.keySet()); (line 434)
//        List<String> staged2 = Blob.getStagedBlobs();
//        for (int i = 0; i<staged2.size(); i++) { //staged is names of files
//            File a = Utils.join(Main.STAGEDDIR, staged2.get(i)); //blob file
//            Blob b = Utils.readObject(a, Blob.class);
//            File c = Utils.join(Main.CURRENTDIR, b.getFilename()); //actual file
//            //with different contents than in the working directory
//            if (b.changed()) {
//                NotStaged.add(c.getName() + " (modified)");
//            }
//            //Deleted in the working directory;
//            else if (!(c.isFile())) {
//                NotStaged.add(c.getName() + " (deleted)");
//            }
//        }
//
//        Collections.sort(NotStaged);
//        for (String i: NotStaged) {
//            System.out.println(i);
//        }
        System.out.println();

        //##################################################################
        // files present ***in the working directory*** but neither staged for addition nor tracked
        //includes files that have been staged for removal, but then re-created without Gitlet’s knowledge.
        System.out.println("=== Untracked Files ===");

//        ArrayList<String> RepoFiles = new ArrayList(Utils.plainFilenamesIn(Main.CURRENTDIR)); //list of files/directories in repository
//        ArrayList<String> Untracked = new ArrayList<>();
//        for (int i = 0; i < RepoFiles.size(); i++) {
//            File a = Utils.join(Main.CURRENTDIR, RepoFiles.get(i));
//            String fileName = a.getName();
//            Character first = fileName.charAt(0);
//            //So that hidden files are not listed
//            if (first.equals('.')) {
//                continue;
//            }
//            if (!(Blob.tracked(a)) && !(staged.contains(fileName))) {
//                Untracked.add(fileName);
//            }
//            if (Blob.removed.keySet().contains(a.getName())) {
//                Untracked.add(fileName);
//            }
//        }
//        Collections.sort(Untracked);
//        for (String i: Untracked) {
//            System.out.println(i);
//        }
    }


    public static void checkout(String commitID, String fileName, String branch) throws IOException {


        //if checking out a file (checkout -- [file name])
        if ((commitID == null) && (branch == null)) {
            File documentFile = Utils.join(Main.CURRENTDIR, fileName);
            Commit latestCommit = Commit.fromFile(getHeadCommitID());

            if (latestCommit.getDictionary().containsKey(fileName)){
                String blobIDtoCommit = (String) latestCommit.getDictionary().get(fileName);
                Blob fileBlob = Blob.getBlob(Utils.join(BLOBDIR, blobIDtoCommit));
                Utils.writeContents(documentFile, fileBlob.getDocumentRead());
                return;
            } else {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
        }

        //if checking out file FROM a commit (checkout [commit id] -- [file name])
        if (branch == null) {

            String fullCommitID =  Commit.getCommitIDFromAbreviated(commitID);
            File documentFile = Utils.join(Main.CURRENTDIR, fileName);
            Commit commitToRestore = Commit.fromFile(fullCommitID);

            List<String> allCommits = Utils.plainFilenamesIn(Main.COMMITDIR);

            if (!(commitToRestore.getDictionary().containsKey(fileName))) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            if (!(allCommits.contains(fullCommitID))) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            else {
                String blobIDtoCommit = (String) commitToRestore.getDictionary().get(fileName);
                Blob fileBlob = Blob.getBlob(Utils.join(BLOBDIR, blobIDtoCommit));
                Utils.writeContents(documentFile, fileBlob.getDocumentRead());
                return;
            }
            //setBranchPointer(commitID, getHeadBranchName()); <- it's the same as 1st checkout except from another commitID rather than Current commitID
            //return;
        }

        //if checking out a branch (checkout [branch name])
        else {
            if (getHeadBranchName().equals(branch)) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }
            List<String> branches = Utils.plainFilenamesIn(BRANCHESDIR);
            if (!(branches.contains(branch))){
                System.out.println("No such branch exists.");
                System.exit(0);
            }

            Commit latestCommit = Commit.fromFile(getBranchPointer(branch));
            HashMap<String, String> filesToReplace = latestCommit.getDictionary();
            Set<String> files = filesToReplace.keySet();

            //perform this check before anything else.
            for (String i : files) {
                File blobs = Utils.join(BLOBDIR, filesToReplace.get(i));
                Blob Bloby = Utils.readObject(blobs, Blob.class);
                File location = Bloby.location(); //Utils.join(CURRENTDIR, filesToReplace.get(i));

                if (!(Blob.tracked(location)) && location.isFile()) {
                    Blob temp = new Blob(location);
                    if (!(temp.Sha1().equals(Bloby.Sha1()))) {
                        System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                        System.exit(0);
                    }
                }
            }
            for (String i : files) {
                String blob_name = filesToReplace.get(i);
                File blobs = Utils.join(BLOBDIR, blob_name);
                Blob Bloby = Utils.readObject(blobs, Blob.class);
                Merge.replace(Bloby);
            }

            List<String> directoryFiles = Utils.plainFilenamesIn(CURRENTDIR);
            for (String i : directoryFiles) {
                if (!(files.contains(i))) {
                    File deleter = Utils.join(CURRENTDIR, i);
                    deleter.delete();
                }
            }
            setHeadPointer(branch);
            Blob.clearStagingArea();
        }

    }

    public static void merge(String branchName) throws IOException {
        if (!(Blob.removed.isEmpty() || Blob.file_dict.isEmpty())) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (getHeadBranchName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        List<String> allBranches = getAllBranches();
        if (!(allBranches.contains(branchName))) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        boolean mergeConflict = false;
        ArrayList<String> RepoFiles = new ArrayList(Utils.plainFilenamesIn(Main.CURRENTDIR));

        //Concerning commit and dictionary of HEAD commit
        File headlocation = Utils.join(COMMITDIR, getHeadCommitID());
        Commit nowHead = Utils.readObject(headlocation, Commit.class);
        HashMap<String,String> nowDict = nowHead.getDictionary();
        Set<String> trackedfiles = nowDict.keySet(); //Note: RepoFiles are tracked

        //Concerning commit and dictionary of (given) Branch name
        File branchlocation = Utils.join(COMMITDIR, getBranchPointer(branchName));
        Commit branchHead = Utils.readObject(branchlocation, Commit.class);
        HashMap<String,String> branchDict = branchHead.getDictionary();
        Set<String> toBeMerged = branchDict.keySet();

        //concerning commit of splitpoint
        Commit splitpoint = Merge.findSplitPoint(nowHead, branchHead);
        HashMap<String,String> splitDict = splitpoint.getDictionary();
        Set<String> splitkey = splitDict.keySet();

        //if splitpoint is same commit as head of given branch
        if (splitpoint.getSha1().equals(branchHead.getSha1())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        //split point is the current branch
        if (splitpoint.getSha1().equals(nowHead.getSha1())) {
            System.out.println("Current branch fast-forwarded.");
            checkout(null, null, branchName);
            System.exit(0);
        }

        //perform this check before anything else.
        for (String i : toBeMerged) {
            File blobs = Utils.join(BLOBDIR, branchDict.get(i));
            Blob Bloby = Utils.readObject(blobs, Blob.class);
            File location = Bloby.location();

            if (!(Blob.tracked(location)) && location.isFile()) {
                Blob temp = new Blob(location);
                if (!(temp.Sha1().equals(Bloby.Sha1()))){
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        for (String i : toBeMerged) { //names of files in current directory, same as tracked files in branch
            File BlobLoc = Utils.join(BLOBDIR, branchDict.get(i));
            Blob branchFile = Utils.readObject(BlobLoc, Blob.class);
            File branchFileLocation = branchFile.location();

            //if is added file in given branch (didn't exist before
            if (!(nowDict.containsKey(i)) && !(splitDict.containsKey(i))) {
                checkout(branchHead.getSha1(), i, null);
                addFileToStaging(branchFileLocation);
                continue;
            }

            //if splitpoint commit contains whatever filename
            if (splitDict.containsKey(i)) {
                if (splitDict.get(i).equals(branchDict.get(i))) { //if split's file is identical to mergebranch's file, skip it
                    continue;
                }

                //if not in current directory, so just add it
                else if (!(nowDict.containsKey(i))) {
                    checkout(getBranchPointer(branchName), i, null);
                    //Merge.replace(branchFile);
                    addFileToStaging(branchFileLocation);
                    System.out.println("some unaccounted scenario 1 ");
                    continue;
                }
                else if (nowDict.get(i).equals(splitDict.get(i))) { //mergeBranch's file not same as split's, but currFile is same as split's
                    checkout(getBranchPointer(branchName), i, null);
                    //Merge.replace(branchFile);
                    addFileToStaging(branchFileLocation);
                    continue;
                }
                else if (!(branchDict.get(i).equals(splitDict.get(i))) && (!(branchFileLocation.isFile()) || (branchFileLocation.isFile() && !(nowDict.get(i).equals(splitDict.get(i))))) ) {
                    File tempLoc = Utils.join(BLOBDIR, "temp");
                    tempLoc.createNewFile();
                    Blob temp = new Blob(tempLoc);
                    Utils.writeObject(tempLoc, temp);
                    Blob HeadFile = Utils.readObject(Utils.join(BLOBDIR, "temp"), Blob.class);
                    Blob branchFile2 = Utils.readObject(Utils.join(BLOBDIR, branchDict.get(i)), Blob.class);
                    Merge.mergeFile(HeadFile, branchFile2);
                    addFileToStaging(HeadFile.location());
                    mergeConflict = true;
                    tempLoc.delete();
                    continue;
                }
            }
        }

        for (String i : trackedfiles) {
            if (splitDict.containsKey(i) && splitDict.get(i).equals(branchDict.get(i))) { //if split's file is identical to mergebranch's file, skip it
                continue;
            }
            if (toBeMerged.contains(i)) {
                //skip if contents are the same
                if (branchDict.get(i).equals(nowDict.get(i))) {
                    continue;
                }
                else {
                    Blob HeadFile = Utils.readObject(Utils.join(BLOBDIR, nowDict.get(i)), Blob.class);
                    Blob branchFile = Utils.readObject(Utils.join(BLOBDIR, branchDict.get(i)), Blob.class);
                    Merge.mergeFile(HeadFile, branchFile);
                    addFileToStaging(HeadFile.location());
                    mergeConflict = true;
                    continue;
                }
            }
            else { //if files toBeMerged doesn't contain i
                if (!(nowDict.get(i).equals(splitDict.get(i)))) {
                    File tempLoc = Utils.join(BLOBDIR, "temp");
                    tempLoc.createNewFile();
                    Blob temp = new Blob(tempLoc);
                    Utils.writeObject(tempLoc, temp);
                    Blob HeadFile = Utils.readObject(Utils.join(BLOBDIR, "temp"), Blob.class);
                    Blob branchFile2 = Utils.readObject(Utils.join(BLOBDIR, nowDict.get(i)), Blob.class);
                    Merge.mergeFile(branchFile2, HeadFile);
                    addFileToStaging(branchFile2.location());
                    mergeConflict = true;
                    tempLoc.delete();
                    continue;
                }
                if (splitDict.containsKey(i) && nowDict.get(i).equals(splitDict.get(i))) {
                    File location = Utils.join(CURRENTDIR, i);
                    removeFilefromStaging(location);
                    continue;
                }
            }
        }

        String log = "Merged " + branchName + " into " + getHeadBranchName() + ".";
        Commit newCommit = new Commit(log, getHeadCommitID(), null, getBranchPointer(branchName));
        Blob.moveStagedFileNames();
        HashMap<String, String> StagedDict = new HashMap<>();
        Utils.writeObject(STAGED, StagedDict);
        Blob.file_dict = new HashMap<>();

        branchHead.setAsSplit();

        HashMap<String, File> removedDict = new HashMap<>();
        Utils.writeObject(REMOVED, removedDict);
        Blob.file_dict = new HashMap<>();

        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }
}



