package client_test

// You MUST NOT change these default imports.  ANY additional imports may
// break the autograder and everyone will be sad.

import (
	// Some imports use an underscore to prevent the compiler from complaining
	// about unused imports.
	_ "encoding/hex"
	"errors"
	_ "strconv"
	ss "strings"
	"testing"

	// A "dot" import is used here so that the functions in the ginko and gomega
	// modules can be used without an identifier. For example, Describe() and
	// Expect() instead of ginko.Describe() and gomega.Expect().
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"

	userlib "github.com/cs161-staff/project2-userlib"

	"github.com/cs161-staff/project2-starter-code/client"
	_ "fmt"

	//added
	"github.com/google/uuid"
	"encoding/json"
	"time"
)

func TestSetupAndExecution(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Client Tests")
}

// ================================================
// Global Variables (feel free to add more!)
// ================================================
const defaultPassword = "password"
const emptyString = ""
const contentOne = "Bitcoin is Nick's favorite "
const contentTwo = "digital "
const contentThree = "cryptocurrency!"


//Client.toBytes isn't working for some reason.
func toBytes(input interface{}) []byte {
	value, err := json.Marshal(input)
	if err != nil {
		panic(errors.New("Error while converting to byte[]: " + err.Error()))
	}
	return value
}

//for adding stuff to Datastore
func addData(key uuid.UUID, stuff interface{}) {
	userlib.DatastoreSet(key, toBytes(stuff))
}

// ================================================
// Describe(...) blocks help you organize your tests
// into functional categories. They can be nested into
// a tree-like structure.
// ================================================

var _ = Describe("Client Tests", func() {

	// A few user declarations that may be used for testing. Remember to initialize these before you
	// attempt to use them!
	var alice *client.User
	var bob *client.User
	var charles *client.User
	// var doris *client.User
	// var eve *client.User
	// var frank *client.User
	// var grace *client.User
	// var horace *client.User
	// var ira *client.User

	// These declarations may be useful for multi-session testing.
	var alicePhone *client.User
	var aliceLaptop *client.User
	var aliceDesktop *client.User

	var err error

	// A bunch of filenames that may be useful.
	aliceFile := "aliceFile.txt"
	bobFile := "bobFile.txt"
	charlesFile := "charlesFile.txt"
	// dorisFile := "dorisFile.txt"
	// eveFile := "eveFile.txt"
	// frankFile := "frankFile.txt"
	// graceFile := "graceFile.txt"
	// horaceFile := "horaceFile.txt"
	// iraFile := "iraFile.txt"

	BeforeEach(func() {
		// This runs before each test within this Describe block (including nested tests).
		// Here, we reset the state of Datastore and Keystore so that tests do not interfere with each other.
		// We also initialize
		userlib.DatastoreClear()
		userlib.KeystoreClear()
	})

	Describe("Basic Tests", func() {

		Specify("Basic Test: Testing InitUser/GetUser on a single user.", func() {
			userlib.DebugMsg("Initializing user Alice.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Getting user Alice.")
			aliceLaptop, err = client.GetUser("alice", defaultPassword)
			Expect(err).To(BeNil())
		})

		Specify("Basic Test: Testing Single User Store/Load/Append.", func() {
			userlib.DebugMsg("Initializing user Alice.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Storing file data: %s", contentOne)
			err = alice.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Appending file data: %s", contentTwo)
			err = alice.AppendToFile(aliceFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Appending file data: %s", contentThree)
			err = alice.AppendToFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Loading file...")
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))
		})

		Specify("Basic Test: Testing Create/Accept Invite Functionality with multiple users and multiple instances.", func() {
			userlib.DebugMsg("Initializing users Alice (aliceDesktop) and Bob.")
			aliceDesktop, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Getting second instance of Alice - aliceLaptop")
			aliceLaptop, err = client.GetUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("aliceDesktop storing file %s with content: %s", aliceFile, contentOne)
			err = aliceDesktop.StoreFile(aliceFile, []byte(contentOne))
			Expect(err).To(BeNil())

			userlib.DebugMsg("aliceLaptop creating invite for Bob.")
			invite, err := aliceLaptop.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			userlib.DebugMsg("Bob accepting invite from Alice under filename %s.", bobFile)
			err = bob.AcceptInvitation("alice", invite, bobFile)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Bob appending to file %s, content: %s", bobFile, contentTwo)
			err = bob.AppendToFile(bobFile, []byte(contentTwo))
			Expect(err).To(BeNil())

			userlib.DebugMsg("aliceDesktop appending to file %s, content: %s", aliceFile, contentThree)
			err = aliceDesktop.AppendToFile(aliceFile, []byte(contentThree))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Checking that aliceDesktop sees expected file data.")
			data, err := aliceDesktop.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))

			userlib.DebugMsg("Checking that aliceLaptop sees expected file data.")
			data, err = aliceLaptop.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))

			userlib.DebugMsg("Checking that Bob sees expected file data.")
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))

			userlib.DebugMsg("Getting third instance of Alice - alicePhone.")
			alicePhone, err = client.GetUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Checking that alicePhone sees Alice's changes.")
			data, err = alicePhone.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne + contentTwo + contentThree)))
		})

		Specify("Basic Test: Testing Revoke Functionality", func() {
			userlib.DebugMsg("Initializing users Alice, Bob, and Charlie.")
			alice, err = client.InitUser("alice", defaultPassword)
			Expect(err).To(BeNil())

			bob, err = client.InitUser("bob", defaultPassword)
			Expect(err).To(BeNil())

			charles, err = client.InitUser("charles", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Alice storing file %s with content: %s", aliceFile, contentOne)
			alice.StoreFile(aliceFile, []byte(contentOne))

			userlib.DebugMsg("Alice creating invite for Bob for file %s, and Bob accepting invite under name %s.", aliceFile, bobFile)

			invite, err := alice.CreateInvitation(aliceFile, "bob")
			Expect(err).To(BeNil())

			err = bob.AcceptInvitation("alice", invite, bobFile)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			userlib.DebugMsg("Checking that Bob can load the file.")
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			userlib.DebugMsg("Bob creating invite for Charles for file %s, and Charlie accepting invite under name %s.", bobFile, charlesFile)
			invite, err = bob.CreateInvitation(bobFile, "charles")
			Expect(err).To(BeNil())

			err = charles.AcceptInvitation("bob", invite, charlesFile)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Checking that Charles can load the file.")
			data, err = charles.LoadFile(charlesFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			userlib.DebugMsg("Alice revoking Bob's access from %s.", aliceFile)
			err = alice.RevokeAccess(aliceFile, "bob")
			Expect(err).To(BeNil())

			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err = alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			userlib.DebugMsg("Checking that Bob/Charles lost access to the file.")
			_, err = bob.LoadFile(bobFile)
			Expect(err).ToNot(BeNil())

			_, err = charles.LoadFile(charlesFile)
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("Checking that the revoked users cannot append to the file.")
			err = bob.AppendToFile(bobFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			err = charles.AppendToFile(charlesFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())
		})

	})
	


	//Custom tests


	
	Describe("Usersessions", func() {
		Specify("Custom Test: InitUser", func() {
			userlib.DebugMsg("Empty Username == bad")
			_, err := client.InitUser("", defaultPassword)
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("Empty passwords == good")
			_, err = client.InitUser("A", "")
			Expect(err).To(BeNil())
			_, err = client.GetUser("A", "")
			Expect(err).To(BeNil())

			userlib.DebugMsg("User with same pass == good")
			userpass := string(userlib.RandomBytes(16))

			_, err = client.InitUser("B", userpass)
			Expect(err).To(BeNil())

			_, err = client.InitUser("C", userpass)
			Expect(err).To(BeNil())

			userlib.DebugMsg("User with same name == bad")
			_, err = client.InitUser("A", "")
			Expect(err).ToNot(BeNil())
			_, err = client.InitUser("A", userpass)
			Expect(err).ToNot(BeNil())
			_, err = client.InitUser("B", userpass)
			Expect(err).ToNot(BeNil())
		})
		Specify("Custom Test: GetUser Errors", func() {

			username := string(userlib.RandomBytes(16))
			userpass := string(userlib.RandomBytes(16))

			userlib.DebugMsg("User not initialized")
			_, err = client.GetUser(username, userpass)
			Expect(err).ToNot(BeNil())

			_, err = client.InitUser(username, userpass)
			Expect(err).To(BeNil())


			userlib.DebugMsg("Wrong passwords")
			_, err = client.GetUser(username, "asdfa")
			Expect(err).ToNot(BeNil())

			_, err = client.GetUser(username, "")
			Expect(err).ToNot(BeNil())

			A, err := client.GetUser(username, userpass)
			Expect(err).To(BeNil())
			A.StoreFile(aliceFile, []byte(contentOne))

			userlib.DebugMsg("Wrong username")
			_, err = client.GetUser("B", userpass)
			Expect(err).ToNot(BeNil())
			_, err = client.GetUser("akjahwefkjnklwv", userpass)
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("Any password length")
			_, err = client.InitUser("A", ss.Repeat("A", 100000))
			Expect(err).To(BeNil())
			_, err = client.GetUser("A", ss.Repeat("A", 100000))
			Expect(err).To(BeNil())


			/*
			userlib.DebugMsg("Compromised")
			userUUID := client.UUIDerr(userlib.Hash([]byte(username)))


			temp := A.Salt
			A.Salt = []byte("no")
			addData(userUUID, A)

			_, err = client.GetUser(username, userpass)
			Expect(err).ToNot(BeNil())


			A.Salt = temp
			temp = A.Check
			A.Check = []byte("no")
			addData(userUUID, A)

			_, err = client.GetUser(username, userpass)
			Expect(err).ToNot(BeNil())


			A.Check = temp
			temp = A.Salt2
			A.Salt2 = []byte("no")
			addData(userUUID, A)

			_, err = client.GetUser(username, userpass)
			Expect(err).ToNot(BeNil())


			A.Salt2 = temp
			temp2 := A.Files
			A.Files = [][]byte{[]byte("nonononononononnonoonononononononon")}
			addData(userUUID, A)

			_, err = client.GetUser(username, userpass)
			Expect(err).ToNot(BeNil())

			A.Files = temp2
			temp = A.Check2
			A.Check2 = []byte("asdf")
			addData(userUUID, A)

			_, err = client.GetUser(username, userpass)
			Expect(err).ToNot(BeNil())

			A.Check2 = temp
			addData(userUUID, A)
			_, err = client.GetUser(username, userpass)
			Expect(err).To(BeNil())



			userlib.DebugMsg("Detect Keyring changes")
			keyRingID := client.UUIDerr(userlib.Argon2Key([]byte(userpass + username), A.Salt2, 64))

			temp, ok := userlib.DatastoreGet(keyRingID)
			Expect(ok).To(Equal(true))

			var keydata client.Keyring
			err = json.Unmarshal(temp, &keydata)
			Expect(err).To(BeNil())

			temp = keydata.Verification
			keydata.Verification = []byte("nonononononononnonoonononononononon")
			addData(keyRingID, keydata)

			_, err = client.GetUser(username, userpass)
			Expect(err).ToNot(BeNil())
			*/

		})
	})

	Describe("FileStoring Verification", func() {
		Specify("Custom Test: Any filename length.", func() {
			userlib.DebugMsg("Any filename length.")
			A, err := client.InitUser("A", defaultPassword)
			Expect(err).To(BeNil())

			name1 := ""
			file1 := "0"

			name2 := ss.Repeat("A", 5000)
			file2 := "5000"

			name3 := ss.Repeat("A", 1000000)
			file3 := "1000000"

			userlib.DebugMsg("Store file1")
			err = A.StoreFile(name1, []byte(file1))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Store file2")
			err = A.StoreFile(name2, []byte(file2))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Store file3")
			err = A.StoreFile(name3, []byte(file3))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Load file1")
			data, err := A.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file1)))

			userlib.DebugMsg("Load file2")
			data, err = A.LoadFile(name2)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file2)))

			userlib.DebugMsg("Load file3")
			data, err = A.LoadFile(name3)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file3)))
		})

		Specify("Custom Test: Any file content length.", func() {
			userlib.DebugMsg("Any file content length + verify contents.")

			A, err := client.InitUser("A", defaultPassword)
			Expect(err).To(BeNil())

			name1 := "1"
			file1 := ""

			name2 := "2"
			file2 := userlib.RandomBytes(23)

			name3 := "3"
			file3 := userlib.RandomBytes(10000)

			userlib.DebugMsg("Store file1")
			err = A.StoreFile(name1, []byte(file1))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Store file2")
			err = A.StoreFile(name2, []byte(file2))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Store file3")
			err = A.StoreFile(name3, []byte(file3))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Load file1")
			data, err := A.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file1)))

			userlib.DebugMsg("Load file2")
			data, err = A.LoadFile(name2)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file2)))

			userlib.DebugMsg("Load file3")
			data, err = A.LoadFile(name3)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file3)))


			userlib.DebugMsg("replacing file3 and 2, 1 should not be changed")
			userlib.DebugMsg("replacing file3")
			err = A.StoreFile(name3, []byte(file2))
			Expect(err).To(BeNil())

			userlib.DebugMsg("replace file2")
			err = A.StoreFile(name2, []byte("why"))
			Expect(err).To(BeNil())

			userlib.DebugMsg("load file3")
			data, err = A.LoadFile(name3)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file2)))

			userlib.DebugMsg("Load file1")
			data, err = A.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file1)))

			userlib.DebugMsg("Load file2")
			data, err = A.LoadFile(name2)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte("why")))


			userlib.DebugMsg("Loading file that doesn't exist")
			data, err = A.LoadFile("4")
			Expect(err).ToNot(BeNil())
		})


		/*
		Specify("Custom Test: FIle modified", func() {
				
			//The integrity of the downloaded content cannot be verified (indicating 
			//there have been unauthorized modifications to the file).
			//Check FilePointer, File, FileAddon. 
			###

		})
		*/
	})
	

	Describe("File append and replace", func() {
		Specify("Custom Test: Share Chain Revoke. Multiuser while you're at it", func() {
			userlib.DebugMsg("Appending timer stuff + multiuser.")

			A, err := client.InitUser("A", defaultPassword)

			A1, err := client.GetUser("A", defaultPassword)
			Expect(err).To(BeNil())


			Expect(err).To(BeNil())

			name1 := "1"
			file1 := ss.Repeat("A", 500)
			file2 := ss.Repeat("B", 100000)
			file3 := ss.Repeat("C", 1100000)
			file4 := ss.Repeat("D", 11100000)
			name2 := "2"

			userlib.DebugMsg("Store file1")
			err = A.StoreFile(name1, []byte(file1))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Store file2")
			err = A1.StoreFile(name2, []byte("when"))
			Expect(err).To(BeNil())

			A2, err := client.GetUser("A", defaultPassword)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Append1")
			start := time.Now()
			err = A2.AppendToFile(name1, []byte(file2))
			stop := time.Since(start)
			Expect(err).To(BeNil())
			//userlib.DebugMsg(fmt.Sprint(stop))
			//time for 100000
			root := stop

			//Verifying loadFile
			data, err := A.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file1 + file2)))


			userlib.DebugMsg("Append2")
			start = time.Now()
			err = A.AppendToFile(name1, []byte(file3))
			stop = time.Since(start)
			Expect(err).To(BeNil())
			//userlib.DebugMsg(fmt.Sprint(stop - root))
			Expect((stop - root) <= root*10).To(Equal(true))
			//Time for 1100000
			root2 := stop
			//Time for 1000000
			root = stop - root

			//Verifying loadFile
			data, err = A1.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file1 + file2 + file3)))


			userlib.DebugMsg("Append3")
			start = time.Now()
			err = A2.AppendToFile(name1, []byte(file4))
			stop = time.Since(start)
			Expect(err).To(BeNil())
			//userlib.DebugMsg(fmt.Sprint(stop - root2))
			//userlib.DebugMsg(fmt.Sprint(root*10))
			Expect((stop - root2) <= root*10).To(Equal(true))

			//Verifying loadFile
			data, err = A2.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file1 + file2 + file3 + file4)))



			userlib.DebugMsg("Replacing Appended files.")

			userlib.DebugMsg("Load file2")
			data, err = A.LoadFile(name2)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte("when")))

			userlib.DebugMsg("Add stuff to file2")
			err = A2.AppendToFile(name2, []byte(file3))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Verify file2")
			data, err = A1.LoadFile(name2)
			Expect(err).To(BeNil())
			Expect(data).To(Equal(append([]byte("when"), []byte(file3)...)))

			userlib.DebugMsg("replacing file1")
			err = A.StoreFile(name1, []byte(file2))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Load file1")
			data, err = A2.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file2)))

			userlib.DebugMsg("Load file2")
			data, err = A.LoadFile(name2)
			Expect(err).To(BeNil())
			Expect(data).To(Equal(append([]byte("when"), []byte(file3)...)))


			userlib.DebugMsg("Checking to see that you can Append to replaced files")
			userlib.DebugMsg("replacing file2")
			err = A.StoreFile(name2, []byte("when"))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Adding stuff")
			err = A1.AppendToFile(name2, []byte("asdf"))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Adding nothing")
			//"content may include empty sequence."
			err = A.AppendToFile(name2, []byte(""))
			Expect(err).To(BeNil())

			userlib.DebugMsg("Load file2")
			data, err = A2.LoadFile(name2)
			Expect(err).To(BeNil())
			Expect(data).To(Equal(append([]byte("whenasdf"))))
		})

		/*
		Specify("Custom Test: MultiUser", func() {
			Performed tests above
		})
		*/

		
		Specify("Custom Test: Append fails", func() {
			A, err := client.InitUser("A", "A")
			Expect(err).To(BeNil())

			B, err := client.InitUser("B", "B")
			Expect(err).To(BeNil())

			_, err = client.InitUser("C", "C")
			Expect(err).To(BeNil())

			name1 := "1"
			file1 := []byte("when")

			//name1 = "1"
			file2 := []byte("why")

			userlib.DebugMsg("Store file1 in both A and B")
			err = A.StoreFile(name1, file1)
			Expect(err).To(BeNil())
			err = A.StoreFile("2A", file2)
			Expect(err).To(BeNil())

			err = B.StoreFile(name1, file1)
			Expect(err).To(BeNil())
			err = B.StoreFile("2B", file2)
			Expect(err).To(BeNil())

			userlib.DebugMsg("append to file that user doesn't own -> error")
			err = A.AppendToFile("2B", []byte("No"))
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("load file that user doesn't own -> error")
			data, err := A.LoadFile("2B")
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("Append to own file even though same name as another user's")
			err = A.AppendToFile(name1, []byte("A"))
			Expect(err).To(BeNil())

			userlib.DebugMsg("A's changes")
			data, err = A.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal(append([]byte(file1), []byte("A")...)))

			userlib.DebugMsg("B's remain same")
			data, err = B.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file1)))


			//Later when sharing:
			//appending to ownername rather than username should fail
			//appended file should be shown to others
			//fail if 

			userlib.DebugMsg("Inviting")
			_, err = A.CreateInvitation(name1, "B")
			Expect(err).To(BeNil())

			invite1B, err := B.CreateInvitation(name1, "A")
			Expect(err).To(BeNil())
			invite2B, err := B.CreateInvitation("2B", "A")
			Expect(err).To(BeNil())

			userlib.DebugMsg("User should not access file before accept invite")
			data, err = A.LoadFile("2B")
			Expect(err).ToNot(BeNil())
			//A's own file is unchanged after invite despite same owner name
			data, err = A.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal(append([]byte(file1), []byte("A")...)))

			userlib.DebugMsg("Invited person doesn't exist")
			_, err = A.CreateInvitation(name1, "D")
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("Invited File doesn't exist in namespace")
			//Note: don't invite back. Creates a loop
			_, err = A.CreateInvitation("2B", "B")
			Expect(err).ToNot(BeNil())


			//A accepts 
			userlib.DebugMsg("Accept fail due to same file name in namespace")
			err = A.AcceptInvitation("B", invite1B, name1)
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("Accept fail due to wrong senderUsername(doesn't exist")
			err = A.AcceptInvitation("D", invite1B, "asdf")
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("Accept fail due to wrong senderUsername (doesn't match)")
			err = A.AcceptInvitation("C", invite1B, "asdf")
			Expect(err).ToNot(BeNil())

			//Accepting invite
			err = A.AcceptInvitation("B", invite1B, "asdf")
			Expect(err).To(BeNil())

			userlib.DebugMsg("old name1 file access for A doesn't change")
			data, err = A.LoadFile(name1)
			Expect(err).To(BeNil())
			Expect(data).To(Equal(append([]byte(file1), []byte("A")...)))

			userlib.DebugMsg("B appends stuff to 1B")
			err = B.AppendToFile(name1, []byte("B"))
			Expect(err).To(BeNil())

			userlib.DebugMsg("A sees changes")
			data, err = A.LoadFile("asdf")
			Expect(err).To(BeNil())
			Expect(data).To(Equal(append([]byte(file1), []byte("B")...)))


			
			userlib.DebugMsg("Revoke File not in user's space, error")
			err = B.RevokeAccess("2A", "A")
			Expect(err).ToNot(BeNil())
			userlib.DebugMsg("Revoke Filename not in user's space, error")
			err = B.RevokeAccess("asdf", "A")
			Expect(err).ToNot(BeNil())
			userlib.DebugMsg("Revoke User doesn't exist, error")
			err = B.RevokeAccess("2B", "D")
			Expect(err).ToNot(BeNil())
			userlib.DebugMsg("Revoke User not being shared with, error")
			err = B.RevokeAccess("2B", "C")
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("Revoking 2B from A (not completely shared)")
			err = B.RevokeAccess("2B", "A")
			Expect(err).To(BeNil())

			//Accepting invite
			userlib.DebugMsg("A can't accept 2B invite now")
			err = A.AcceptInvitation("B", invite2B, "2B")
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("A can't access 2B")
			data, err = A.LoadFile("2B")
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("B's 2B remain same")
			data, err = B.LoadFile("2B")
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(file2)))



			//Edge case of user accepting own invite

			//
		})

		/*
		Specify("Custom Test: Invite integrity test", func() {
			userlib.DebugMsg("Invite integrity test")

			A, err := client.InitUser("A", "A")
			Expect(err).To(BeNil())

			B, err := client.InitUser("B", "B")
			Expect(err).To(BeNil())

			name1 := "1"
			file1 := []byte("when")

			userlib.DebugMsg("Store file1 in A")
			err = A.StoreFile(name1, file1)
			Expect(err).To(BeNil())


			/*
			keyRingID := client.UUIDerr(userlib.Argon2Key([]byte("BB"), B.Salt2, 64))

			temp, ok := userlib.DatastoreGet(keyRingID)
			Expect(ok).To(Equal(true))

			var keydata client.Keyring
			err = json.Unmarshal(temp, &keydata)
			Expect(err).To(BeNil())
			*/

			/*
			userlib.DebugMsg("A invites B to have 1")
			invite, err := A.CreateInvitation(name1, "B")
			Expect(err).To(BeNil())

			userlib.DebugMsg("Weird accept invite (userhash mismatch probably)")
			err = A.AcceptInvitation("B", invite, name1)
			Expect(err).ToNot(BeNil())


			var Invitation client.Invite 
			temp, ok := userlib.DatastoreGet(invite)
			Expect(ok).To(Equal(true))
			err = json.Unmarshal(temp, &Invitation)
			Expect(err).To(BeNil())

			//temp, err := userlib.PKEDec(keydata.Priv2, Invitation.Verification)
			temp = Invitation.Verification
			Invitation.Verification = []byte("AAA")
			addData(invite, Invitation)

			userlib.DebugMsg("Invite Verification integrity")
			err = B.AcceptInvitation("A", invite, name1)
			Expect(err).ToNot(BeNil())


			Invitation.Verification = temp 
			temp = Invitation.Senderhash
			Invitation.Senderhash = []byte("AAA")
			addData(invite, Invitation)

			userlib.DebugMsg("Invite Senderhash integrity")
			err = B.AcceptInvitation("A", invite, name1)
			Expect(err).ToNot(BeNil())


			Invitation.Senderhash = temp 
			temp = Invitation.OwnerFileH
			Invitation.OwnerFileH = []byte("AAA")
			addData(invite, Invitation)

			userlib.DebugMsg("Invite OwnerFileH integrity")
			err = B.AcceptInvitation("A", invite, name1)
			Expect(err).ToNot(BeNil())
			

			Invitation.OwnerFileH = temp 
			temp = Invitation.OwnerNameH
			Invitation.OwnerNameH = []byte("AAA")
			addData(invite, Invitation)

			userlib.DebugMsg("Invite OwnerNameH integrity")
			err = B.AcceptInvitation("A", invite, name1)
			Expect(err).ToNot(BeNil())
			

			Invitation.OwnerNameH = temp 
			temp = Invitation.FileSymKey
			Invitation.FileSymKey = []byte("AAA")
			addData(invite, Invitation)

			userlib.DebugMsg("Invite FileSymKey integrity")
			err = B.AcceptInvitation("A", invite, name1)
			Expect(err).ToNot(BeNil())


			Invitation.FileSymKey = temp 
			temp = Invitation.FileHKey
			Invitation.FileHKey = []byte("AAA")
			addData(invite, Invitation)

			userlib.DebugMsg("Invite FileHKey integrity")
			err = B.AcceptInvitation("A", invite, name1)
			Expect(err).ToNot(BeNil())

			Invitation.FileHKey = temp
			addData(invite, Invitation)
			err = B.AcceptInvitation("A", invite, name1)
			Expect(err).To(BeNil())
		})
		*/

	})

	/*
	Describe("Share Chain", func() {

		Specify("Custom Test: Revoke 1 but not other", func() {
			userlib.DebugMsg("Initializing users A, B, C")
			A, err = client.InitUser("A", defaultPassword)
			Expect(err).To(BeNil())

			B, err = client.InitUser("B", defaultPassword)
			Expect(err).To(BeNil())

			C, err = client.InitUser("C", defaultPassword)
			Expect(err).To(BeNil())


			nameA1 := "A1"
			file1 := "A1"

			nameA2 := "A2"
			file2 := "A2"

			userlib.DebugMsg("A storing file %s with content: %s", nameA1, file1)
			A.StoreFile(nameA1, []byte(file1))

			userlib.DebugMsg("A storing file %s with content: %s", nameA2, file2)
			A.StoreFile(nameA2, []byte(file2))

			invite, err := A.CreateInvitation(nameA1, "B")
			Expect(err).To(BeNil())
			err = B.AcceptInvitation("alice", invite, nameB)
			Expect(err).To(BeNil())



			invite, err := A.CreateInvitation(nameA2, "B")
			Expect(err).To(BeNil())

			Expect(err).ToNot(BeNil())

		})

		Specify("Custom Test: Share Chain Revoke.", func() {
			userlib.DebugMsg("Initializing users A, b, C, D, E")
			A, err = client.InitUser("A", defaultPassword)
			Expect(err).To(BeNil())

			B, err = client.InitUser("B", defaultPassword)
			Expect(err).To(BeNil())

			C, err = client.InitUser("C", defaultPassword)
			Expect(err).To(BeNil())

			D, err = client.InitUser("C", defaultPassword)
			Expect(err).To(BeNil())

			E, err = client.InitUser("C", defaultPassword)
			Expect(err).To(BeNil())


			nameA1 := "A1"
			file1 := "A1"

			nameA2 := "A2"
			file2 := "A2"


			err = alice.RevokeAccess(aliceFile, "bob")


			userlib.DebugMsg("Alice storing file %s with content: %s", nameA1, file1)
			A.StoreFile(nameA1, []byte(file1))

			userlib.DebugMsg("Alice storing file %s with content: %s", nameA2, file2)
			A.StoreFile(nameA2, []byte(file2))



			userlib.DebugMsg("Alice creating invite for Bob for file %s, and Bob accepting invite under name %s.", aliceFile, bobFile)

			invite, err := A.CreateInvitation(name1, "bob")
			Expect(err).To(BeNil())

			invite, err := A.CreateInvitation(name1, "bob")
			Expect(err).To(BeNil())

			nameB := "B1"

			err = B.AcceptInvitation("alice", invite, nameB)
			Expect(err).To(BeNil())


			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err := alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			userlib.DebugMsg("Checking that Bob can load the file.")
			data, err = bob.LoadFile(bobFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			userlib.DebugMsg("Bob creating invite for Charles for file %s, and Charlie accepting invite under name %s.", bobFile, charlesFile)
			invite, err = bob.CreateInvitation(bobFile, "charles")
			Expect(err).To(BeNil())

			err = charles.AcceptInvitation("bob", invite, charlesFile)
			Expect(err).To(BeNil())

			userlib.DebugMsg("Checking that Charles can load the file.")
			data, err = charles.LoadFile(charlesFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			userlib.DebugMsg("Alice revoking Bob's access from %s.", aliceFile)
			err = alice.RevokeAccess(aliceFile, "bob")
			Expect(err).To(BeNil())

			userlib.DebugMsg("Checking that Alice can still load the file.")
			data, err = alice.LoadFile(aliceFile)
			Expect(err).To(BeNil())
			Expect(data).To(Equal([]byte(contentOne)))

			userlib.DebugMsg("Checking that Bob/Charles lost access to the file.")
			_, err = bob.LoadFile(bobFile)
			Expect(err).ToNot(BeNil())

			_, err = charles.LoadFile(charlesFile)
			Expect(err).ToNot(BeNil())

			userlib.DebugMsg("Checking that the revoked users cannot append to the file.")
			err = bob.AppendToFile(bobFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())

			err = charles.AppendToFile(charlesFile, []byte(contentTwo))
			Expect(err).ToNot(BeNil())
		})
	})
	*/

	/*
	Describe("testing", func() {
		Specify("Btesting", func() {
			userlib.DebugMsg("testingTesting")

			//pub, priv, err := userlib.PKEKeyGen()
			//pub2, _, err := userlib.PKEKeyGen()
			//if err != nil {userlib.DebugMsg("What the")}
			var testing []byte = userlib.RandomBytes(32)
			userlib.DebugMsg(fmt.Sprint(len(testing)))
			//bytes, err := json.Marshal(pub)
			//if err != nil {userlib.DebugMsg("What the x2")}

			//userlib.DebugMsg("key 1")
			//cip := userlib.Hash(testing)
			//if err != nil {userlib.DebugMsg("What the")}
			//userlib.DebugMsg(string(cip)[:])
			//userlib.DebugMsg(fmt.Sprint(len(cip[:14])))

			PointSymKey := userlib.RandomBytes(16)
			cip := userlib.SymEnc(PointSymKey, userlib.RandomBytes(16), testing)

			userlib.DebugMsg(fmt.Sprint(len(cip)))
			//cip, err := userlib.PKEEnc(pub, testing)
			//if err != nil {userlib.DebugMsg("What the")}

			userlib.DebugMsg(string(cip)[:])

			//res, _ := userlib.PKEDec(priv, cip)
			res := userlib.SymDec(PointSymKey, cip)
			userlib.DebugMsg(string(res)[:])
			userlib.DebugMsg(fmt.Sprint(len(res)))
		})
	})
	*/
})
