package gitlet;
import java.io.File;
import com.sun.source.tree.AssertTree;
import org.junit.Assert;
import ucb.junit.textui;
import org.junit.Test;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    @Test
    public void deleteEverything() {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
    }

    @Test
    public void basicInitTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }

        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        assertTrue(Main.GITDIR.exists());
        assertTrue(Main.BLOBDIR.exists());
        List<String> fileName = Utils.plainFilenamesIn(Main.COMMITDIR);
        assertTrue((Utils.plainFilenamesIn(Main.COMMITDIR)).size() == 1);
        String shaValueInitialCommit = fileName.get(0);
        Commit a = Commit.fromFile(shaValueInitialCommit);
        assertTrue(a.getMessage().equals("initial commit"));
        assertTrue(a.getParent() == null);

    }

    @Test
    public void simpleAddTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File test = new File("testFile.txt");
        test.createNewFile();
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),"Test");
        String[] add = new String[]{"add", "testFile.txt" };
        Main.main(add);
        assertTrue(Utils.plainFilenamesIn(Main.STAGEDDIR).size() == 1);
        assertTrue(Utils.plainFilenamesIn(Main.BLOBDIR).size() == 0);
        Main.main(add);
        assertTrue(Utils.plainFilenamesIn(Main.STAGEDDIR).size() == 1);
        assertTrue(Utils.plainFilenamesIn(Main.BLOBDIR).size() == 0);
        assertTrue(Utils.plainFilenamesIn(Main.COMMITDIR).size() == 1);
        test.delete();
        Main.deleteDirectory(Main.GITDIR);
    }

    @Test
    public void AddTest2() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);

        File a = Utils.join(Main.CURRENTDIR, "a.txt");
        Utils.writeContents(a, "a");
        File b = Utils.join(Main.CURRENTDIR, "b.txt");
        Utils.writeContents(b, "b");
        Main.main("add", "a.txt");
        Main.main("add", "b.txt");
        System.out.println(Blob.file_dict); //normal dict with a and b

        Main.main("commit", "add a&b");
        Main.main("add", "a.txt");
        System.out.println(Blob.file_dict);
        Assert.assertTrue(Blob.file_dict.equals(new HashMap<>())); //blank, tracked, no changes, don't add.

        Utils.writeContents(b, "bees");
        Main.main("add", "b.txt");
        System.out.println(Blob.file_dict); //should have b.txt.

        Utils.writeContents(a, "aasdf");
        Utils.writeContents(b, "b");
        Main.main("add", "b.txt");
        System.out.println(Blob.file_dict);
        Assert.assertTrue(Blob.file_dict.equals(new HashMap<>())); //blank, tracked, no changes, remove from staging.

        Utils.writeContents(b, "boi");
        Main.main("add", "a.txt");
        Main.main("add", "b.txt");
        System.out.println(Blob.file_dict); //both files listed, different code

        Main.deleteDirectory(Main.GITDIR);
    }

    @Test
    public void simpleCommitTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File test = new File("testFile.txt");
        test.createNewFile();
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),"Test");
        String[] add = new String[]{"add", "testFile.txt" };
        Main.main(add);
        String[] commitTest = new String[]{"commit", "test commit comment"};
        Main.main(commitTest);
        assertTrue(Utils.plainFilenamesIn(Main.STAGEDDIR).size() == 0);
        assertTrue(Utils.plainFilenamesIn(Main.BLOBDIR).size() == 1);
        assertTrue(Utils.plainFilenamesIn(Main.COMMITDIR).size() == 2);
        test.delete();

//        Main.deleteDirectory(Main.GITDIR);

    }

    @Test
    public void multipleChangesCommitFolderSizeTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File test = new File("testFile.txt");
        test.createNewFile();
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),"This is a tst sentence");
        String[] add = new String[]{"add", "testFile.txt" };
        Main.main(add);
        String[] firstRealCommit = new String[]{"commit", "First real commit"};
        Main.main(firstRealCommit);
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),"This is a test sentence");
        Main.main(add);
        String[] secondRealCommit = new String[]{"commit", "fixed spelling"};
        Main.main(secondRealCommit);
        assertTrue(Utils.plainFilenamesIn(Main.STAGEDDIR).size() == 0);
        assertTrue(Utils.plainFilenamesIn(Main.BLOBDIR).size() == 2);
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),
                "This is a test sentence. I am in CS61BL");
        Main.main(add);
        String[] thirdRealCommit = new String[]{"commit", "add a sentence"};
        Main.main(thirdRealCommit);
        assertTrue(Utils.plainFilenamesIn(Main.STAGEDDIR).size() == 0);
        assertTrue(Utils.plainFilenamesIn(Main.BLOBDIR).size() == 3);
        test.delete();


        Main.deleteDirectory(Main.GITDIR);


    }

    @Test
    public void stagingSameVersionJustCommittedTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),"This is a tst sentence");
        String[] add = new String[]{"add", "testFile.txt" };
        Main.main(add);
        String[] firstRealCommit = new String[]{"commit", "First real commit"};
        Main.main(firstRealCommit);
        Main.main(add);
        assertTrue(Utils.plainFilenamesIn(Main.STAGEDDIR).size() == 0);

        Main.deleteDirectory(Main.GITDIR);
    }

    @Test
    public void stagingDifferentVersionsSameFileTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),"This is a tst sentence");
        String[] add = new String[]{"add", "testFile.txt" };
        Main.main(add);
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),"This is a test sentence");
        Main.main(add);
        assertTrue(Utils.plainFilenamesIn(Main.STAGEDDIR).size() == 1);

        //just seeing if 2 commits show up
        Main.main("commit", "add tstfile");

    }

    @Test
    public void stagingSameVersionsSameFileTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),"This is a test sentence");
        String[] add = new String[]{"add", "testFile.txt" };
        Main.main(add);
        Main.main(add);
        assertTrue(Utils.plainFilenamesIn(Main.STAGEDDIR).size() == 1);
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
    }

    @Test
    public void logtest() throws IOException, InterruptedException {
        //######WARNING: TAKES ≥2 SEC TO RUN FOR TIMING PURPOSES

        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);

        File test = new File("testFile.txt");
        test.createNewFile();
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "testFile.txt"),"Something about bacon");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add testfile");

        //pause for 1 seconds (to see commit time difference)
        TimeUnit.SECONDS.sleep(1);

        File tst = new File("tstFile.txt");
        tst.createNewFile();
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "tstFile.txt"),"This is a tst sentence");
        Main.main("add", "tstFile.txt");
        Main.main("commit", "add tstfile");
        //3 commits now

        TimeUnit.SECONDS.sleep(1);

        File teeeest = new File("teeeestFile.txt");
        tst.createNewFile();
        Utils.writeContents(Utils.join(Main.CURRENTDIR, "teeeestFile.txt"),"an occasional tea party");
        Main.main("add", "teeeestFile.txt");
        Main.main("commit", "add teeeestfile");

        Main.main("log");

        //Main.main(new String[]{"global-log"}); (returns same stuff as log but not in order)

        //delete everything when done
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
    }

    @Test
    public void statustest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);

        Main.main("status");
        File a = Utils.join(Main.CURRENTDIR, "a.txt");
        Utils.writeContents(a, "a");
        File b = Utils.join(Main.CURRENTDIR, "b.txt");
        Utils.writeContents(b, "b");
        Main.main("add", "a.txt");
        Main.main("add", "b.txt");
        System.out.println("/////////////////////////1///////////////////////////////");
        System.out.println("A and B in 'Staged Files'");
        Main.main("status");

        Main.main("commit", "add a&b");
        System.out.println("/////////////////////////2///////////////////////////////");
        System.out.println("Everything empty after commit");
        Main.main("status");
        Main.main("add", "a.txt"); //doesn't add a.txt cuz no changes
        System.out.println(Blob.file_dict);
        assertEquals(Blob.file_dict, new HashMap<>()); //blank, tracked, no changes, don't add.
        System.out.println("/////////////////////////3///////////////////////////////");
        System.out.println("Add unchanged file, should still be empty");
        Main.main("status");

        Utils.writeContents(b, "bees");
        System.out.println("/////////////////////////4///////////////////////////////");
        System.out.println("B added to 'Modifications Not Staged'");
        Main.main("status");
        Main.main("add", "b.txt");

        Utils.writeContents(a, "aasdf");
        Utils.writeContents(b, "b");
        Main.main("add", "b.txt");
        System.out.println(Blob.file_dict); //blank
        System.out.println("/////////////////////////5///////////////////////////////");
        System.out.println("A in 'Modifications Not Staged', B not in there (or anywhere) anymore");
        Main.main("status");

        Utils.writeContents(b, "boi");
        Main.main("add", "a.txt");
        Main.main("add", "b.txt");
        //both files listed, different code
        System.out.println("/////////////////////////6///////////////////////////////");
        System.out.println("A and B in 'Staged'");
        Main.main("status");

        Main.deleteDirectory(Main.GITDIR);


        //delete everything when done
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
    }

    /**"Short" test to see if Blob dictionary thing works**/
    @Test
    public void dictionary() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "a.txt");
        Utils.writeContents(a, "a");
        File b = Utils.join(Main.CURRENTDIR, "b.txt");
        Utils.writeContents(b, "b");
        Main.main("add", "a.txt");
        System.out.println(Blob.file_dict);
        Main.main("add", "b.txt");
        System.out.println(Blob.file_dict);
        String save = Blob.file_dict.get("b.txt");

        Utils.writeContents(b, "bees");
        Main.main("add", "b.txt");
        System.out.println(Blob.file_dict);
        System.out.println(new ArrayList(Utils.plainFilenamesIn(Main.CURRENTDIR)));
        String save2 = (String) Blob.file_dict.get("b.txt");
        assertNotEquals(save, save2);

        Main.main("commit", "add tstfile");
        assertEquals(Blob.file_dict, new Hashtable());

        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
    }

    /** creates an file with some sample text in the working directory, then adds+ commits. More changes
     * are made, then a call to checkout on that file is made. Should revert file back to previous version from last
     * commit
     * @throws IOException
     */
    @Test
    public void checkingOutFileWithoutCommitIDTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "This is a tst sentence");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        Utils.writeContents(a,"This is a newer  version of the file");
        Main.main("checkout", "--","testFile.txt");
        assertTrue(Utils.readContentsAsString(a).equals("This is a tst sentence"));

        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }

    }

    @Test
    public void checkoutWithShortCommitID() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "This is a tst sentence");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        String oldCommitID = Main.getHeadCommitID();
        String shortenedOldCommitID = oldCommitID.substring(0, 9);
        Utils.writeContents(a, "Newer test sentence");
        Main.main("add", "testFile.txt");
        Main.main("commit", "newer testfile");
        Utils.writeContents(a,"This is a newer  version of the file");
        Main.main("checkout", oldCommitID, "--", "testFile.txt");
        String fileRead = Utils.readContentsAsString(Utils.join(Main.CURRENTDIR, "testFile.txt"));
        assertTrue(fileRead.equals("This is a tst sentence"));
    }

    @Test
    public void simpleFindTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "This is a tst sentence");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        Utils.writeContents(a, "This is a new tst sentence");
        Main.main("add", "testFile.txt");
        Main.main("commit", "modified");

        Main.main("find", "\"add tstfile\"");
        Main.main("find", "modified   ");
        //two commit IDs should print
    }

    /**testing rm**/
    @Test
    public void rm() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        System.out.println("not staged or saved in commit. should return 'no reason'");
        Main.main("rm", "a.txt");
        File a = Utils.join(Main.CURRENTDIR, "a.txt");
        Utils.writeContents(a, "a");
        File b = Utils.join(Main.CURRENTDIR, "b.txt");
        Utils.writeContents(b, "b");
        System.out.println("Not staged. should return 'no reason'");
        Main.main("rm", "b.txt");
        Assert.assertTrue(Blob.file_dict.equals(new Hashtable()));

        Main.main("add", "a.txt");
        Main.main("add", "b.txt");
        System.out.println("A and B staged");
        System.out.println(Blob.file_dict);
        Main.main("rm", "a.txt"); //remove a.txt from staged, no print though.
        System.out.println("A removed from Staging, but not committed so not deleted");
        System.out.println(Blob.file_dict); //No "a.txt"
        Assert.assertTrue(a.isFile());

        Main.main("add", "a.txt");
        Main.main("commit", "add a and b");
        System.out.println("A removed AND deleted, Staging is blank because just committed");
        Main.main("rm", "a.txt");
        assertFalse(a.isFile());
        System.out.println(Blob.file_dict);

        File c = Utils.join(Main.CURRENTDIR, "c.txt");
        Utils.writeContents(c, "c");
        Main.main("add", "c.txt");
        HashMap a1 = Blob.file_dict;
        System.out.println("File C added to Staging");
        System.out.println(Blob.file_dict);
        Main.main("rm", "c.txt");
        System.out.println("File C removed from Staging, but not deleted");
        System.out.println(Blob.file_dict);
        Assert.assertTrue(c.isFile()); // (do not remove it unless it is tracked in the current commit)

        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
    }

    @Test
    public void multipleBranchesCommitTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        Main.main("branch", "new branch");
        Utils.writeContents(a, "new a");
        Main.main("add", "testFile.txt");
        String masterBranchPointer = Main.getBranchPointer("master");
        Main.main("commit", "add new a");
        String masterBranchPointer1 = Main.getBranchPointer("master");
        String headCommitID = Main.getHeadCommitID(); ///shoudld still be MAIN
        String newBranchcommitID = Main.getBranchPointer("new branch");

        assertFalse(headCommitID.equals(newBranchcommitID));

    }

    @Test
    public void resetTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
    }

//    @Test
//    public void findSplitPointTest() {
//
//    }

    /** checks basic operations of checking out a branch where there
     * are no splits
     * @throws IOException
     */

    @Test
    public void basicCheckoutBranchTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        String headCommitID = Main.getHeadCommitID();
        Main.main("branch", "new branch");
        Utils.writeContents(a, "new content");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add new contents");
        Main.main("checkout", "new branch");
        assertTrue(Utils.readContentsAsString(a).equals("a"));
        assertTrue(Main.getHeadBranchName().equals("new branch"));
        assertTrue(Main.getBranchPointer("new branch").equals(headCommitID));
        assertTrue(Utils.plainFilenamesIn(Main.STAGEDDIR).size() == 0);

    }

    /** test to check that java exits if we attempt to checkout the current branch
     * Last line needs to be uncommented, and this test MUST be run individually*/

    @Test
    public void checkoutCurrentBranchTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        String headCommitID = Main.getHeadCommitID();
        Main.main("branch", "new branch");
        Utils.writeContents(a, "new content");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add new contents");

        //uncomment to test this specific test

//        Main.main("checkout", "master");
        //should print: no need to checkout the current branch.
        // then should exit

    }


    /** test to check that java exits if we attempt to checkout when
     * there are unstaged files that would be overwritten.
     * Last line must be uncommented and this test MUST  be run individually*/

    @Test
    public void checkoutBranchWithUnstagedFilesTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        File b = Utils.join(Main.CURRENTDIR, "testFile1.txt");
        Utils.writeContents(b, "b");
        Main.main("add", "testFile.txt");
        Main.main("add", "testFile1.txt");
        Main.main("commit", "added both text files");
//        String headCommitID = Main.getHeadCommitID();
        Main.main("branch", "new branch");
        Utils.writeContents(a, "new content");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add new contents");
        File c = Utils.join(Main.CURRENTDIR, "testFile2.txt");
        Utils.writeContents(c, "c");

        Main.main("add", "testFile2.txt");
        Main.main("commit", "add testFile2");

        //uncomment to test this specific test
        System.out.println(Main.getHeadBranchName());
        System.out.println();

        Main.main("checkout", "new branch");
//        test should print: There is an untracked file in the way;
        // delete it, or add and commit it first.
        //
        // then should exit

    }

    @Test
    public void checkoutBranchOnlyOneCommitAheadNoUntracked() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        Main.main("branch", "new branch");
        File b = Utils.join(Main.CURRENTDIR, "testFile2.txt");
        Utils.writeContents(b, "this is file b");
        Main.main("add", "testFile2.txt");
        Main.main("commit", "add new contents");
        Main.main("checkout", "new branch");
        assertTrue(Utils.readContentsAsString(a).equals("a"));

    }

    /** tests a simple removal of a branch, with no untracked files
     * that would have been overwritten
     */

    @Test
    public void removeBranchTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        Main.main("branch", "new branch");
        Utils.writeContents(a, "new version of a");
        Main.main("rm-branch", "new branch");
        assertTrue(Utils.plainFilenamesIn(Main.BRANCHESDIR).size() == 1);
    }


    @Test
    public void removeNonExistentBranchTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        Main.main("branch", "new branch");
        Utils.writeContents(a, "new version of a");

        //uncomment this out to test
//        Main.main("rm-branch", "new brancheee");

        //should print "A branch with that name does not exist" and exit
    }

    /** attempts to remove current branch when branch is the starting branch */

    @Test
    public void removeCurrentBranch1() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);

        //uncomment this out to test
//        Main.main("rm-branch", "master");
        //should print "Cannot Remove current branch" and exit

    }
/** attempts to checkout different branch same initial commit */
    @Test
    public void checkoutInitalCommit() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        Main.main("branch", "new branch");
        Main.main("checkout", "new branch");
        assertTrue(Main.getHeadBranchName().equals("new branch"));

    }


    /** tests to checkout different branch same initial commit */
    @Test
    public void checkoutInitalCommit2() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);
        Main.main("branch", "new branch");

        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        Utils.writeContents(a, "new a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        Main.main("checkout", "new branch");
        Main.deleteDirectory(Main.GITDIR);
        Main.main("init");
        Main.main("branch", "new branch");
        File b = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(b, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile");
        Main.main("checkout", "new branch");
        assertTrue(Main.getHeadBranchName().equals("new branch"));
        assertFalse(b.isFile());

    }


        /** attempts to checkout different branch that points to initial commit
         *
         */

        @Test
        public void checkoutInitalCommit3() throws IOException {
            if (Main.GITDIR.exists()) {
                Main.deleteDirectory(Main.GITDIR);
            }
            String[] initTest = new String[]{"init"};
            Main.main(initTest);
            Main.main("branch", "new branch");
            Main.main("checkout", "new branch");
            assertTrue(Main.getHeadBranchName().equals("new branch"));

        }



    /** attempts to checkout different branch that is split from current branch
     *
     */

    @Test
    public void checkoutSplitBranch() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);

        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile1");
        Main.main("branch", "new branch");
        Utils.writeContents(a, "new a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile2");
        Main.main("checkout", "new branch");
        Utils.writeContents(a, "new aaaaaa");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile3");
        Main.main("checkout", "master");
        assertTrue(Utils.readContentsAsString(a).equals("new a"));

    }

    /** testing for fastforwarding: checking out a branch that is on same branch as current, but ahead */
    @Test
    public void fastForwardTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);

        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile1");
        Main.main("branch", "new branch");
        Utils.writeContents(a, "new a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile2");
        Utils.writeContents(a, "new a 2");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile3");
        Main.main("checkout", "new branch");
        Main.main("checkout", "master");
        assertTrue(Utils.readContentsAsString(a).equals("new a 2"));
    }




    /** testing for relative location from current head */
    @Test
    public void relativePositionTest() throws IOException {
        if (Main.GITDIR.exists()) {
            Main.deleteDirectory(Main.GITDIR);
        }
        String[] initTest = new String[]{"init"};
        Main.main(initTest);

        File a = Utils.join(Main.CURRENTDIR, "testFile.txt");
        Utils.writeContents(a, "a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile1");
//        Main.main("branch", "new branch");
        Utils.writeContents(a, "new a");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile2");
        String sha1SecondCommit = Main.getHeadCommitID();
        Utils.writeContents(a, "new a 2");
        Main.main("add", "testFile.txt");
        Main.main("commit", "add tstfile3");
//        Main.main("checkout", "new branch");
//        Main.main("checkout", "master");

        assertTrue(Commit.checkRelativeLocation(Commit.fromFile(sha1SecondCommit)).equals("downstream"));
    }






}

