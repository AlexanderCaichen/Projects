package client

// CS 161 Project 2

// You MUST NOT change these default imports. ANY additional imports
// may break the autograder!

import (
	"encoding/json"

	userlib "github.com/cs161-staff/project2-userlib"
	"github.com/google/uuid"

	// hex.EncodeToString(...) is useful for converting []byte to string

	// Useful for string manipulation
	//"strings"

	// Useful for formatting strings (e.g. `fmt.Sprintf`).
	"fmt"

	// Useful for creating new error messages to return using errors.New("...")
	"errors"

	// Optional.
	_ "strconv"
)

// This serves two purposes: it shows you a few useful primitives,
// and suppresses warnings for imports not being used. It can be
// safely deleted!
func someUsefulThings() {

	// Creates a random UUID.
	randomUUID := uuid.New()

	// Prints the UUID as a string. %v prints the value in a default format.
	// See https://pkg.go.dev/fmt#hdr-Printing for all Golang format string flags.
	userlib.DebugMsg("Random UUID: %v", randomUUID.String())

	// Creates a UUID deterministically, from a sequence of bytes.
	hash := userlib.Hash([]byte("user-structs/alice"))
	deterministicUUID, err := uuid.FromBytes(hash[:16])
	if err != nil {
		// Normally, we would `return err` here. But, since this function doesn't return anything,
		// we can just panic to terminate execution. ALWAYS, ALWAYS, ALWAYS check for errors! Your
		// code should have hundreds of "if err != nil { return err }" statements by the end of this
		// project. You probably want to avoid using panic statements in your own code.
		panic(errors.New("An error occurred while generating a UUID: " + err.Error()))
	}
	userlib.DebugMsg("Deterministic UUID: %v", deterministicUUID.String())

	// Declares a Course struct type, creates an instance of it, and marshals it into JSON.
	type Course struct {
		name      string
		professor []byte
	}

	course := Course{"CS 161", []byte("Nicholas Weaver")}
	courseBytes, err := json.Marshal(course)
	if err != nil {
		panic(err)
	}

	userlib.DebugMsg("Struct: %v", course)
	userlib.DebugMsg("JSON Data: %v", courseBytes)

	// Generate a random private/public keypair.
	// The "_" indicates that we don't check for the error case here.
	var pk userlib.PKEEncKey
	var sk userlib.PKEDecKey
	pk, sk, _ = userlib.PKEKeyGen()
	userlib.DebugMsg("PKE Key Pair: (%v, %v)", pk, sk)

	// Here's an example of how to use HBKDF to generate a new key from an input key.
	// Tip: generate a new key everywhere you possibly can! It's easier to generate new keys on the fly
	// instead of trying to think about all of the ways a key reuse attack could be performed. It's also easier to
	// store one key and derive multiple keys from that one key, rather than
	originalKey := userlib.RandomBytes(16)
	derivedKey, err := userlib.HashKDF(originalKey, []byte("mac-key"))
	if err != nil {
		panic(err)
	}
	userlib.DebugMsg("Original Key: %v", originalKey)
	userlib.DebugMsg("Derived Key: %v", derivedKey)

	// A couple of tips on converting between string and []byte:
	// To convert from string to []byte, use []byte("some-string-here")
	// To convert from []byte to string for debugging, use fmt.Sprintf("hello world: %s", some_byte_arr).
	// To convert from []byte to string for use in a hashmap, use hex.EncodeToString(some_byte_arr).
	// When frequently converting between []byte and string, just marshal and unmarshal the data.
	//
	// Read more: https://go.dev/blog/strings

	// Here's an example of string interpolation!
	_ = fmt.Sprintf("%s_%d", "file", 1)
}

// This is the type definition for the User struct.
// A Go struct is like a Python or Java class - it can have attributes
// (e.g. like the Username attribute) and methods (e.g. like the StoreFile method below).
//
	// You can add other attributes here if you want! But note that in order for attributes to
	// be included when this struct is serialized to/from JSON, they must be capitalized.
	// On the flipside, if you have an attribute that you want to be able to access from
	// this struct's methods, but you DON'T want that value to be included in the serialized value
	// of this struct that's stored in datastore, then you can use a "private" variable (e.g. one that
	// begins with a lowercase letter).

//Key: hash of username
//Don't user hash of user+pass (no protection against hashtable thing, salt slows stuff down)
type User struct {
	//For verifying password
	Salt []byte
	//Argon2Key of password and Salt
	Check []byte

	//For Getting corresponding Keyring struct
	//+Files keys then HMaced with keyring.FilesMacKey to get Check2
	//Argon2Key([]byte(password + username), userdata.Salt2) to get keyring UUID
	Salt2 []byte

	//PKEDecKey -> json.Marshal to byte[] then encrypted via public key (1)
	//PKEDecKey is for decrypting contents of FilePointer


	//(Hash of name of file in personal namespace) + (Hash of name of file given by owner) + (Hash of owner name of file) 
	//Value : (Sym Key (for FilePointer) + Hmac Key (For File verification))
	//Encrypt Value using Public key 1 (from Keyring)
	//		- HMac key is 16 byte, Sym key is 16
	//Cuz map[string][]byte does not get Unraveled correctly

	//(Hash of name of file in personal namespace) + (Hash of name of file given by owner) + (Hash of owner name of file) + (Sym Key (for FilePointer&File) + Hmac Key (For File verification))
	//64 + 64 + 64 + 16 + 16
	//The 16+16 is encrypted via Pub1
	Files [][]byte
	//HMac(Salt2 + Files's "keys") via Keyring's FileMacKey
	//For verifying User integrity.
	Check2 []byte 

	//^Owner of file must be able to modify FileSymKey and FileHKey in Files


	//THIS IS NOT SAVED IN STORAGE
	keyring uuid.UUID
	userhash string


	//Username string
}

//Key: Argon2Key([]byte(password + username), userdata.Salt2, 64)
type Keyring struct {
	Verification []byte

	Priv1 userlib.PKEDecKey
	Priv2 userlib.PKEDecKey
	Priv3 userlib.PKEDecKey
	Priv4 userlib.PKEDecKey

	//For Files in User
	//Symkey []byte
	//iv []byte
	FilesMacKey []byte

	InvitSignfKey userlib.DSSignKey

	//HAS NO USE (Other users other than owner must be able to generate verification for Shared)
	ShareMacKey []byte
}

//Hash(Hash(Owner Filename) + Hash(Owner name))
type FilePointer struct {
	//UUID of File struct encrypted through 
	//EncKey userlib.PKEEncKey

	FileLoc []byte
}

//Random Key
type File struct {
	//List of hashes of people who have access to the file (including owner)
	//Encrypted via SymKey in User
	Access [][]byte

	//why don't we all just have the same private key?
	//Encrypted via SymKey in User
	//Used to decrypt FileAddon contents
	SymKey []byte

	//Hmac's output, key is in User.Files
	Verification []byte

	//UUIDs of FileAddons encrypted with File.SymKey (Or BitSymKey)
	Shards [][]byte



	//Address to Invite structs
	//Why we need: find way to access Invite struct to revoke if revoke before accept invite
	//	- Resharing is undefined behavior
	//Invite entry is deleted if Invite is accepted or 
	//
	//Hash of recipient name + address of corresponding Invite struct (64+16)
	//^Encrypt via owner Pub2
	Invitations [][]byte

	//Encrypt each Shared.Recipient item via owner Pub3
	//Tree Shared
}

//Hash of (Pseudorandomly generated value + arbitrary string)
type FileAddon struct {
	//Hmac output, key is is User.Files
	Verification []byte
	Contents []byte
}


//key: Randomly gen value
type Invite struct {
	//encrypted asymmetrically using invited person’s Public Key #2

	//Hmac Key (From File, or FileHKey) 
	Verification []byte
	//(Hash of sendername)
	Senderhash []byte
	//(Hash of name of file given by owner)
	OwnerFileH []byte
	//(Hash of owner name of file)
	OwnerNameH []byte

	FileSymKey []byte
	FileHKey []byte
}

//Hash((Hash of username) + (Hash of name of file given by owner) + (Hash of owner name of file))
//Why create this struct? It's easier to add connections and search for connections this way.
//	- Don't need to iterate an entire tree everytime we add a Share connection
type Shared struct {
	//List of Hashes of user usernames that local User is sharing file with, aencrypted through Owner’s Public Key #3s. 

	//Encrypt via owner Pub3
	Giver []byte
	Recipients [][]byte

	Verification []byte
}



//###################################################################################################
// NOTE: The following methods have toy (insecure!) implementations.

//turns byte array to UUID
func UUIDerr(hash []byte) (result uuid.UUID) {
	result, err := uuid.FromBytes(hash[:16])
	if err != nil {
		panic(errors.New("An error occurred while generating a UUID: " + err.Error()))
	}
	return result
}

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

//Get from Datastore
func getData(key uuid.UUID) (value []byte, err error){
	//userlib.DatastoreGet(key UUID) (value []byte, ok bool)
	value, ok := userlib.DatastoreGet(key)
	if ok {
		return value, err
	} else {
		return nil, errors.New("Key " + key.String() + " does not exist in DataStore")
	}
}

func getKeyRing(key uuid.UUID) (keydata Keyring, err error) {
	//If Keyring struct doesn't exist, then there's a problem.
 	//if this can't be found, data has been manipulated.
 	temp, err := getData(key)
	if err != nil { return keydata, err }

	//var keydata Keyring
	err = json.Unmarshal(temp, &keydata)
	//Can't unpack byte array into actual Keyring
 	if err != nil { return keydata, errors.New("Error turning byte[] to Keyring: " + err.Error())}

 	return keydata, err
}

//Get from keystore
func getKey(key string) (value userlib.PKEEncKey, err error){
	//userlib.KeystoreGet(key string) (value PKEEncKey/DSVerifyKey, ok bool)
	value, ok := userlib.KeystoreGet(key)
	if ok {
		return value, nil
	} else {
		return value, errors.New("Key " + key + " does not exist in KeyStore")
	}
}

//turns string into Hash string
//Mostly for getKey inputs
func hashString(input string) string {
	return string(userlib.Hash([]byte(input))[:])
}

func appendThree(a []byte, b []byte, c []byte) (output []byte) {
	temp := append(a, b...)
	return append(temp, c...)
}

func appendFour(a []byte, b []byte, c []byte, d []byte) (output []byte) {
	return append(appendThree(a, b, c), d...)
}

func appendFive(a []byte, b []byte, c []byte, d []byte, e []byte) (output []byte) {
	return append(appendFour(a, b, c, d), e...)
}

func appendSeven(a []byte, b []byte, c []byte, d []byte, e []byte, f []byte, g []byte) (output []byte) {
	return append(appendThree(a, b, c), appendFour(d, e, f, g)...)
}

func (userdata *User) UpdateUser() (err error) {
	//update userdata
	val, err := getData( UUIDerr([]byte(userdata.userhash)))
	if err != nil { return err }
	err = json.Unmarshal( val, &userdata)
 	if err != nil { 
 		return errors.New("Error turning byte[] to User: " + err.Error())
 	}
 	return
}


//###################################################################################################


func InitUser(username string, password string) (userdataptr *User, err error) {
	//Creates a new User struct and returns a pointer to it. 
	//The User struct should include all data required by the client to operate on behalf of the user.
	//Returns an error if:
	//A user with the same username exists.
	//An empty username is provided.

	//username == error(nil) ||
	if username == "" {
		return nil, errors.New("empty string input")
	}

	userHash := hashString(username)

	//Check if already exists
	_, ok := userlib.KeystoreGet(userHash)
	if ok {
		return nil, errors.New("UUID already exists for username: " + username)
	}

	var userdata User
	//userdata.Username = username
	userdata.Salt = userlib.RandomBytes(23)
	userdata.Check = userlib.Argon2Key([]byte(password), userdata.Salt, 64)
	userdata.Salt2 = userlib.RandomBytes(256)



	var keydata Keyring

	// 256-byte (doesn't matter)
	//Pair 1
	pub, priv, err := userlib.PKEKeyGen()
	if err != nil {
		//Remove from Keystore???
		return nil, errors.New("Error while generating keys: " + err.Error())
	}
	err = userlib.KeystoreSet(userHash, pub)
	if err != nil {
		return nil, errors.New("Error while inserting to Keystore: " + err.Error())
	}
	keydata.Priv1 = priv

	//Pair 2
	pub, priv, err = userlib.PKEKeyGen()
	if err != nil {
		//Remove from Keystore???
		return nil, errors.New("Error while generating keys: " + err.Error())
	}
	err = userlib.KeystoreSet(userHash + "2", pub)
	if err != nil {
		return nil, errors.New("Error while inserting to Keystore2: " + err.Error())
	}
	keydata.Priv2 = priv

	//Pair 3
	pub, priv, err = userlib.PKEKeyGen()
	if err != nil {
		//Remove from Keystore???
		return nil, errors.New("Error while generating keys: " + err.Error())
	}
	err = userlib.KeystoreSet(userHash + "3", pub)
	if err != nil {
		return nil, errors.New("Error while inserting to Keystore3: " + err.Error())
	}
	keydata.Priv3 = priv

	//Pair 4
	pub, priv, err = userlib.PKEKeyGen()
	if err != nil {
		//Remove from Keystore???
		return nil, errors.New("Error while generating keys: " + err.Error())
	}
	err = userlib.KeystoreSet(userHash + "4", pub)
	if err != nil {
		return nil, errors.New("Error while inserting to Keystore4: " + err.Error())
	}
	keydata.Priv4 = priv


	priv, pub, err = userlib.DSKeyGen()
	if err != nil {
		//Remove from Keystore???
		return nil, errors.New("Error while generating keys: " + err.Error())
	}
	//Technically the public key (for verification)
	err = userlib.KeystoreSet(userHash + "Inv", pub)
	if err != nil {
		return nil, errors.New("Error while inserting to KeystoreInv: " + err.Error())
	}
	//Technically the private key (for signing)
	keydata.InvitSignfKey = priv

	//keydata.Symkey = userlib.RandomBytes(16)

	keydata.FilesMacKey = userlib.RandomBytes(16)
	//Create new hashmap
	userdata.Files = [][]byte{}
	//generate HMAC for User integrity using Salt2 and Files's "keys". Just keys because owner of file mods stuff
	//Salt and Check will check each other
	temp, err := userlib.HMACEval(keydata.FilesMacKey, userdata.Salt2)
	if err != nil {
		return nil, errors.New("Error when creating HMac: " + err.Error())
	}
	userdata.Check2 = temp

	keydata.ShareMacKey = userlib.RandomBytes(16)


	keydata.Verification, err = userlib.HMACEval(userlib.Argon2Key([]byte(password), userdata.Salt, 16), appendSeven(toBytes(keydata.Priv1), toBytes(keydata.Priv2), toBytes(keydata.Priv3), toBytes(keydata.Priv4), keydata.FilesMacKey, toBytes(keydata.InvitSignfKey), keydata.ShareMacKey))
	if err != nil {
		return nil, errors.New("Error when creating HMac: " + err.Error())
	}


	//localized address/data (not stored in DataStore)
	//Address for corresponding Keyring
	key := UUIDerr(userlib.Argon2Key([]byte(password + username), userdata.Salt2, 64))
	userdata.keyring = key

	userdata.userhash = userHash

	addData(UUIDerr([]byte(userHash[:])), userdata)
	addData(key, keydata)

	return &userdata, nil
}

func GetUser(username string, password string) (userdataptr *User, err error) {
	//Obtains the User struct of a user who has already been initialized and returns a pointer to it.
	//Returns an error if:
	//There is no initialized user for the given username.
	//The user credentials are invalid.
	//The User struct cannot be obtained due to malicious action, or the integrity of the user struct has been compromised.

	//Use keystore instead of datastore: keystore can't be modified
	_, ok := userlib.KeystoreGet(hashString(username))
	if !ok {
		return nil, errors.New("Key " + username + " does not exist in KeyStore")
	}

	var userdata User
	val, err := getData( UUIDerr(userlib.Hash([]byte(username))))
	if err != nil { return nil, err }

	err = json.Unmarshal( val, &userdata)
 	if err != nil { 
 		return nil, errors.New("Error turning byte[] to User: " + err.Error())
 	}

 	if !userlib.HMACEqual(userdata.Check, userlib.Argon2Key([]byte(password), userdata.Salt, 64)) {
 		return nil, errors.New("INVALID CREDENTIALS")
 	}

 	/*
 	//This thing isn't saved in DataStore anyways (set later)
 	if userhash != userdata.userhash {
 		return nil, errors.New("MODIFIED USERHASH")
 	}*/


 	//Setting UUID of temp keyring variable
 	userdata.keyring = UUIDerr(userlib.Argon2Key([]byte(password + username), userdata.Salt2, 64))

 	userdata.userhash = hashString(username)


	//Accessing Keyring
 	keydata, err := getKeyRing(userdata.keyring)
 	if err != nil { return nil, err }

 	temp, err := userlib.HMACEval(userlib.Argon2Key([]byte(password), userdata.Salt, 16), appendSeven(toBytes(keydata.Priv1), toBytes(keydata.Priv2), toBytes(keydata.Priv3), toBytes(keydata.Priv4), keydata.FilesMacKey, toBytes(keydata.InvitSignfKey), keydata.ShareMacKey))
	if err != nil {
		return nil, errors.New("Error when creating HMac: " + err.Error())
	}
	if !userlib.HMACEqual(temp, keydata.Verification) { 
		return nil, errors.New("TAMPERED DATA FOR KEYRING") 
	}

	//Verifying integrity of User
 	//Get keys of userdata.Files
	fileCheck := []byte{}
	for _, i := range userdata.Files {
		if len(i) != 448 {
    		return nil, errors.New("Abnormal File entry length")
    	}
		fileCheck = append(fileCheck, i[:192]...)
	}
 	temp, err = userlib.HMACEval(keydata.FilesMacKey, append(userdata.Salt2, fileCheck...))
	if err != nil {
		return nil, errors.New("Error when creating HMac: " + err.Error())
	}
	if !userlib.HMACEqual(temp, userdata.Check2) { 
		return nil, errors.New("TAMPERED DATA FOR USER") 
	}

	return &userdata, nil
}

func getFileStruct(filemap [][]byte, Priv1 userlib.PKEDecKey, fileHash []byte, Userhash []byte) (Resfile File, err error, key []byte, output []byte, fileLoc []byte) {

	var filePointer FilePointer
	var file File

	//[][]byte
	for _, array := range filemap {
    	//fmt.Println("Key:", key, "Value:", value)
    	//(Hash of name of file in personal namespace) + (Hash of name of file given by owner) + (Hash of owner name of file) 

    	if len(array) != 448 {
    		return File{}, errors.New("Abnormal File entry length"), nil, nil, nil
    	}

    	//Hashes are 64 bytes
    	key := array[:192]

    	//Value : (Sym Key (for FilePointer) + Hmac Key (For File verification))
    	value := array[192:]

    	//get name of stored file (personal namespace)
    	if userlib.HMACEqual(key[:64], fileHash) {
    		//Value : (Sym Key (for FilePointer) + Hmac Key (For File verification))
    		//Sym key is 16, HMac key is 16 byte
    		output, err := userlib.PKEDec(Priv1, value)
    		if err != nil { return File{}, err, nil, nil, nil }

    		SymKey := output[:16]

    		//Go to FilePointer
    		//Address is hash of ((Hash of name of file given by owner) + (Hash of owner name of file))
    		temp, err := getData(UUIDerr(userlib.Hash(key[64:])))
			//Can't access FilePointer Address
			if err != nil { return File{}, err, nil, nil, nil }

			err = json.Unmarshal(temp, &filePointer)
			//Can't unpack byte array into actual FilePointer
			if err != nil { return File{}, err, nil, nil, nil }
			//Get address of File using 
			

			//Go to File
			//Decrypt FileLoc using symmetric key
    		temp, err = getData(UUIDerr(userlib.SymDec(SymKey, filePointer.FileLoc)))
			//Can't access File Address
			if err != nil { return File{}, err, nil, nil, nil }

			err = json.Unmarshal(temp, &file)
			//Can't unpack byte array into actual File
			if err != nil { return File{}, err, nil, nil, nil }

			//Verify Hmac for file
			temp, err = userlib.HMACEval(output[16:], appendFour(toBytes(file.Access), file.SymKey, toBytes(file.Shards), toBytes(file.Invitations)))
			if err != nil {
				return File{}, errors.New("Error when creating HMac: " + err.Error()), nil, nil, nil
			}
			if !userlib.HMACEqual(temp, file.Verification) {
				//userlib.DebugMsg(string(temp))
				//userlib.DebugMsg(string(file.Verification))
				return File{}, errors.New("File struct HMac verification failed"), nil, nil, nil
			}

			bad := true
			//Verify you should have access
			for _, i := range file.Access {
				//userlib.DebugMsg(string(Userhash))
				if userlib.HMACEqual(Userhash, userlib.SymDec(SymKey, i)) {
					bad = false
					break 
				}
			}
			if bad {
				return file, errors.New("Shouldn't have access to file"), nil, nil, userlib.SymDec(SymKey, filePointer.FileLoc)
			}

    		return file, nil, key, output, userlib.SymDec(SymKey, filePointer.FileLoc)
    	}
	}
	//can't find, no err
	return File{}, nil, nil, nil, nil
}
/*
func DeleteFiles(shards []string, BitSymkey []byte) () {
	for _, i := range shards {
		userlib.DatastoreDelete(UUIDerr(userlib.SymDec(BitSymkey, []byte(i))))
	}
}
*/

func (userdata *User) StoreFile(filename string, content []byte) (err error) {
	//Given a filename in the personal namespace of the caller, this function persistently stores the given content for future retrieval using the same filename.

	//If the given filename already exists in the personal namespace of the caller, then the content of the corresponding file is overwritten. Note that, in the case of sharing files, the corresponding file may or may not be owned by the caller.

	//The client MUST allow content to be any arbitrary sequence of bytes, including the empty sequence.

	//Note that calling StoreFile after malicious tampering has occurred is undefined behavior, and will not be tested.

	//Returns an error if:
	//The write cannot occur due to malicious action.

	keydata, err := getKeyRing(userdata.keyring)
	if err != nil { return err }

	Priv1 := keydata.Priv1

	Pub1, err := getKey(userdata.userhash)
	//Can't unpack byte array into actual Keyring
	if err != nil { return err }

	err = userdata.UpdateUser()
	if err != nil { return err }

	var fileBit	FileAddon
	var file File
	var FileSymKey []byte
	var FileHKey []byte
	var BitSymkey []byte

	var fileLoc []byte

	fileHash := userlib.Hash([]byte(filename))

	file, err, _, value, fileLoc := getFileStruct(userdata.Files, Priv1, fileHash, []byte(userdata.userhash))

	if value != nil {
    	//Situation 1: file exists, replace file contents (delete FileAddons, clear File's Shards)

		FileSymKey = value[:16]
		FileHKey = value[16:]

		//decrypt file.SymKey, delete shard files
		BitSymkey = userlib.SymDec(FileSymKey, file.SymKey)

		for _, i := range file.Shards {
			userlib.DatastoreDelete(UUIDerr(userlib.SymDec(BitSymkey, i)))
		}
		//userlib.DebugMsg("B" + string(BitSymkey[:]))
	} else if err != nil {
		return err
	} else {
		//Situation 2: file doesn't exist yet (create new FilePointer, File, FileAddon)
		//err and value should be nil. 
		
		//Just can't find file, create new entry
		var filePointer FilePointer

		//New Symmetric key
		FileSymKey = userlib.RandomBytes(16)
		FileHKey = userlib.RandomBytes(16)

		//User Stuff
		newKey := appendThree(fileHash, fileHash, []byte(userdata.userhash))
		//Value : (Sym Key (for FilePointer) + Hmac Key (For File verification))
		newValue, err := userlib.PKEEnc(Pub1, append(FileSymKey, FileHKey...))
		if err != nil { return err }
		//Appending []byte to [][]byte
		userdata.Files = append(userdata.Files, append(newKey, newValue...))
		fileCheck := []byte{}
		for _, i := range userdata.Files {
			if len(i) != 448 {
    			return errors.New("Abnormal File entry length")
    		}
			fileCheck = append(fileCheck, i[:192]...)
		}
		//Get new HMac
	 	temp, err := userlib.HMACEval(keydata.FilesMacKey, append(userdata.Salt2, fileCheck...))
	 	if err != nil {
			return errors.New("Error when creating HMac: " + err.Error())
		}
		userdata.Check2 = temp
		//update userdata
		//userlib.DebugMsg(string(toBytes(userdata.Files)))
		addData(UUIDerr([]byte(userdata.userhash)), userdata)


		//var filePointer FilePointer
		fileLoc = userlib.RandomBytes(16)
		filePointer.FileLoc = userlib.SymEnc(FileSymKey, userlib.RandomBytes(16), fileLoc)

		//Hash of (fileHash, []byte(userdata.userhash))
		addData(UUIDerr(userlib.Hash(newKey[64:])), filePointer)


		//var file File
		file.Access = [][]byte{userlib.SymEnc(FileSymKey, userlib.RandomBytes(16), []byte(userdata.userhash))}
		file.Invitations = [][]byte{}
		//file.Tree = Shared{}

		//SymKey for the shards
		BitSymkey = userlib.RandomBytes(16)
		file.SymKey = userlib.SymEnc(FileSymKey, userlib.RandomBytes(16), BitSymkey)
		//userlib.DebugMsg("A" + string(BitSymkey[:]))
	}

	//replace Shards with new list with new FileAddon, create new Hmac for File

	//New FileAddon address
	fileBitLoc := userlib.RandomBytes(16)
	//New Shard list, contents encrypted with BitSymkey
	file.Shards = [][]byte{userlib.SymEnc(BitSymkey, userlib.RandomBytes(16), fileBitLoc)}
	//generate new HMAC for Shards, SymKey, and Access
	temp, err := userlib.HMACEval(FileHKey, appendFour(toBytes(file.Access), file.SymKey, toBytes(file.Shards), toBytes(file.Invitations)))
	if err != nil {
		return errors.New("Error when creating HMac: " + err.Error())
	}

	file.Verification = temp
	//save File
	addData(UUIDerr(fileLoc), file)


	///////////////////////////////////////////////////////////
	/*
	testerer := userlib.RandomBytes(32)

	var testing testingg
	testing.A = []testingg{}
	testing.B = testerer
	testing.C = 98132750
	testing.D = "Whenever you are"
	testing.E = [][]byte{append(testerer, userlib.RandomBytes(32)...)}

	var testingB testingg
	testingB.A = []testingg{testing}
	testingB.B = userlib.RandomBytes(32)

	arr, err := json.Marshal(testingB)

	userlib.DebugMsg(string(arr))
	var testing2 testingg
	err = json.Unmarshal(arr, &testing2)
	if err != nil { panic(err.Error())}


	userlib.DebugMsg("Original []byte: " + string(toBytes(testingB.A)))
	userlib.DebugMsg("Original []byte: " + string(toBytes(testingB.A[0].A)))
	userlib.DebugMsg("After    []byte: " + string(toBytes(testing2.A)))
	userlib.DebugMsg("After    []byte: " + string(toBytes(testing2.A[0].A)))
	userlib.DebugMsg("Original []str : " + string(toBytes(testingB.B)))
	userlib.DebugMsg("After    []str : " + string(toBytes(testing2.B)))
	//userlib.DebugMsg(fmt.Sprint(testing2.C))
	//userlib.DebugMsg(testing2.D)
	userlib.DebugMsg(string(testerer))
	userlib.DebugMsg(string(testing2.E[0][:32]))
	

	//a, err := getData(UUIDerr([]byte(userdata.userhash)))
	//userlib.DebugMsg(string(a))
	panic(errors.New("Done"))
	*/

	///////////////////////////////////////////////////////////


	//var fileBit	FileAddon
	//Contents are encrypted with BitSymkey (from File)
	fileBit.Contents = userlib.SymEnc(BitSymkey, userlib.RandomBytes(16), content)


	//Hmac for file contents, FileHKey from User
	temp, err = userlib.HMACEval(FileHKey, fileBit.Contents)
	fileBit.Verification = temp
	if err != nil {
		return errors.New("Error when creating HMac: " + err.Error())
	}
	//Save fileAddon
	addData(UUIDerr(fileBitLoc), fileBit)

	return nil
}

type testingg struct {
	A []testingg
	B []byte
	C int 
	D string
	E [][]byte

}

func (userdata *User) AppendToFile(filename string, content []byte) error {
	//Given a filename in the personal namespace of the caller, this function appends the given content to the end of the corresponding file.

	//The client MUST allow content to be any arbitrary sequence of bytes, including the empty sequence.

	//Note that, in the case of sharing files, the corresponding file may or may not be owned by the caller.

	//You are not required to check the integrity of the existing file before appending the new content (integrity verification is allowed, but not required).

	//Returns an error if:

	//The given filename does not exist in the personal file namespace of the caller.
	//Appending the file cannot succeed due to any other malicious action.

	/*
	//Verification that data can be accessed. 
	_, err := getKey(userdata.userhash)
	//Can't unpack byte array into actual Keyring
	if err != nil { return err }
	*/


	keydata, err := getKeyRing(userdata.keyring)
	if err != nil { return err }

	//update userdata
	err = userdata.UpdateUser()
	if err != nil { return err }


	//fileHash := userlib.Hash([]byte(filename))

	file, err, _, value, fileLoc := getFileStruct(userdata.Files, keydata.Priv1, userlib.Hash([]byte(filename)), []byte(userdata.userhash))
	if err != nil {
		return err
	} else if value == nil {
		//Can't access file
		return errors.New("Given filename (" + filename + ") does not exist in the personal file namespace of the caller.")
	} 


	//Nothing to add
	//Why not put this at the front of the func? If user doesn't have access, error. 
	//In the case they're adding blank to a file they can't access, should still error. 
	if len(content) <= 0 {
		return nil
	}

	FileSymKey := value[:16]
	FileHKey := value[16:]

	//replace Shards with new list with new FileAddon, create new Hmac for File

	//New FileAddon address
	fileBitLoc := userlib.RandomBytes(16)
	//decrypt file.SymKey
	BitSymkey := userlib.SymDec(FileSymKey, file.SymKey)
	//Add onto old Shard List
	file.Shards = append(file.Shards, userlib.SymEnc(BitSymkey, userlib.RandomBytes(16), fileBitLoc))
	//generate new HMAC for Shards, SymKey, and Access
	temp, err := userlib.HMACEval(FileHKey, appendFour(toBytes(file.Access), file.SymKey, toBytes(file.Shards), toBytes(file.Invitations)))
	if err != nil {
		return errors.New("Error when creating HMac: " + err.Error())
	}
	file.Verification = temp
	//save File
	addData(UUIDerr(fileLoc), file)


	//Check using userlib.DatastoreGet that fileBitLoc isn't already taken???

	//Save Content in a FileAddon
	var fileBit	FileAddon
	//Contents are encrypted with BitSymkey (from File)
	fileBit.Contents = userlib.SymEnc(BitSymkey, userlib.RandomBytes(16), content)
	//Hmac for file contents, FileHKey from User
	temp, err = userlib.HMACEval(FileHKey, fileBit.Contents)
	if err != nil {
		return errors.New("Error when creating HMac: " + err.Error())
	}
	fileBit.Verification = temp
	addData(UUIDerr(fileBitLoc), fileBit)

	//No problems. 
	return nil
}

func (userdata *User) LoadFile(filename string) (content []byte, err error) {
	//Given a filename in the personal namespace of the caller, this function downloads and returns the content of the corresponding file.

	//Note that, in the case of sharing files, the corresponding file may or may not be owned by the caller.

	//Returns an error if:

	//The given filename does not exist in the personal file namespace of the caller.
	//The integrity of the downloaded content cannot be verified (indicating there have been unauthorized modifications to the file).
	//Loading the file cannot succeed due to any other malicious action.

	keydata, err := getKeyRing(userdata.keyring)
	if err != nil { return nil, err }

	//update userdata
	err = userdata.UpdateUser()
	if err != nil { return nil, err }

	//fileHash := userlib.Hash([]byte(filename))

	file, err, _, value, _ := getFileStruct(userdata.Files, keydata.Priv1, userlib.Hash([]byte(filename)), []byte(userdata.userhash))

	if err != nil {
		return nil, err
	} else if value == nil {
		//Can't access file
		return nil, errors.New("Given filename does not exist in the personal file namespace of the caller.")
	} 

	FileSymKey := value[:16]
	FileHKey := value[16:]

	BitSymkey := userlib.SymDec(FileSymKey, file.SymKey)
	//Decrypt address of each FileAddon, access Addon, verify Addon contents, append Addon contents to result
	for _, i := range file.Shards {
		
		temp, err := getData(UUIDerr(userlib.SymDec(BitSymkey, i)))
		//Can't access FileAddon Value
		if err != nil { 
			//userlib.DebugMsg(string(toBytes(file.Shards)))
			return nil, err 
		}

		var fileBit FileAddon
		err = json.Unmarshal(temp, &fileBit)
		//Can't unpack byte array into actual FineAddon
		if err != nil { return nil, err }

		temp, err = userlib.HMACEval(FileHKey, fileBit.Contents)
		if err != nil {
			return nil, errors.New("Error when creating HMac: " + err.Error())
		}
		if !userlib.HMACEqual(fileBit.Verification, temp) {
			return nil, errors.New("HMac Verification for FileAddon Fail: " + err.Error())
		}

		//Add decrypted contents to result array
		content = append(content, userlib.SymDec(BitSymkey, fileBit.Contents)...)

	}
	//

	/*
	storageKey, err := uuid.FromBytes(userlib.Hash([]byte(filename + userdata.Username))[:16])
	if err != nil {
		return nil, err
	}
	dataJSON, ok := userlib.DatastoreGet(storageKey)
	if !ok {
		return nil, errors.New(strings.ToTitle("file not found"))
	}
	err = json.Unmarshal(dataJSON, &content)
	*/
	return content, nil
}

func (userdata *User) CreateInvitation(filename string, recipientUsername string) (invitationPtr uuid.UUID, err error) {
	//Given a filename in the personal namespace of the caller, this function creates a secure file share invitation that contains all of the information required for recipientUsername to take the actions detailed in Sharing and Revoking on the corresponding file.

	//The returned invitationPtr must be the UUID storage key at which the secure file share invitation is stored in the Datastore.

	//You should assume that, after this function is called, the recipient receives a notification via a secure communication channel that is separate from your client. This notification includes the invitationPtr and the username of the caller who created the invitation.

	//Note that the first parameter to the StoreFile(), LoadFile(), and AppendToFile() functions in the client API is a filename in the caller’s personal namespace. The recipient will not have a name for the shared file in their personal namespace until they accept the invitation by calling AcceptInvitation().

	//You may assume this function will not be called on a recipient who is already currently authorized to access the file (see Sharing and Revoking).

	//Returns an error if:
	//X The given filename does not exist in the personal file namespace of the caller.
	//X The given recipientUsername does not exist.
	//Sharing cannot complete due to any malicious action.

	recUserhash := userlib.Hash([]byte(recipientUsername))

	recPub2, ok := userlib.KeystoreGet(string(recUserhash) + "2")
	if !ok {
		return uuid.UUID{}, errors.New("recipientUsername (" + recipientUsername + ") does not exist in KeyStore")
	}

	keydata, err := getKeyRing(userdata.keyring)
	if err != nil { return uuid.UUID{}, err }

	//update userdata
	err = userdata.UpdateUser()
	if err != nil { return uuid.UUID{}, err }


	file, err, key, value, fileLoc := getFileStruct(userdata.Files, keydata.Priv1, userlib.Hash([]byte(filename)), []byte(userdata.userhash))
	if err != nil {
		return uuid.UUID{}, err
	} else if value == nil {
		//Can't access file
		return uuid.UUID{}, errors.New("File to be shared (" + filename + ") does not exist in the personal file namespace of the caller.")
	}


	//FileSymKey := value[:16]
	FileHKey := value[16:]

	var invitation Invite
	//(Hash of name of file in personal namespace)
	invitation.Senderhash, err = userlib.PKEEnc(recPub2, []byte(userdata.userhash))
	if err != nil { return uuid.UUID{}, err }
	//(Hash of name of file given by owner)
	OwnerFileH := key[64:128]
	invitation.OwnerFileH, err = userlib.PKEEnc(recPub2, OwnerFileH)
	if err != nil { return uuid.UUID{}, err }
	//(Hash of owner name of file)
	OwnerNameH := key[128:]
	invitation.OwnerNameH, err = userlib.PKEEnc(recPub2, OwnerNameH)
	if err != nil { return uuid.UUID{}, err }
	//(Sym Key (for FilePointer&File)
	invitation.FileSymKey, err = userlib.PKEEnc(recPub2, value[:16])
	if err != nil { return uuid.UUID{}, err }
	invitation.FileHKey, err = userlib.PKEEnc(recPub2, FileHKey)
	if err != nil { return uuid.UUID{}, err }


	//Sign struct with user/sender's DSSignKey
	temp, err := userlib.DSSign(keydata.InvitSignfKey, appendFive(invitation.Senderhash, invitation.OwnerFileH, invitation.OwnerNameH, invitation.FileSymKey, invitation.FileHKey))
	if err != nil {
		return uuid.UUID{}, errors.New("Error when creating DIGITAL SIGNATURE: " + err.Error())
	}
	invitation.Verification = temp


	random := userlib.RandomBytes(16)
	newUUID := UUIDerr(random)

	addData(newUUID, invitation)



	//Updating File's Invitation list
	ownerPubKey2, err := getKey(string(key[128:]) + "2")
	if err != nil {
		return uuid.UUID{}, err
	}

	//Encrypt (receiver hash + address to Invite struct) using owner's Public Key 2
	temp, err = userlib.PKEEnc(ownerPubKey2, append(recUserhash, random...))
	if err != nil { return uuid.UUID{}, err }

	//add info to Invitations in File
	file.Invitations = append(file.Invitations, temp)


	temp, err = userlib.HMACEval(FileHKey, appendFour(toBytes(file.Access), file.SymKey, toBytes(file.Shards), toBytes(file.Invitations)))
	if err != nil {
		return uuid.UUID{}, errors.New("Error when creating HMac: " + err.Error())
	}
	file.Verification = temp
	//save File
	addData(UUIDerr(fileLoc), file)



	var share Shared

	ownerPub3, err := getKey(string(OwnerNameH) + "3")
	if err != nil { return uuid.UUID{}, err }

	//Look for Shared object with UUID 
	//Hash((Hash of senderUsername) + (Hash of name of file given by owner) + (Hash of owner name))
	shareUUID := UUIDerr(userlib.Hash(appendThree([]byte(userdata.userhash), OwnerFileH, OwnerNameH)))

	temp, err = getData(shareUUID)
	if err != nil {
		//Share doesn't exist, create new struct
		share.Giver, err = userlib.PKEEnc(ownerPub3, []byte(userdata.userhash))
		if err != nil { return uuid.UUID{}, errors.New("Problem with enc with Pub3:" + err.Error()) }
		share.Recipients = [][]byte{}
	} else {
		//Share exists, so Unmarshal Shared struct
		err = json.Unmarshal(temp, &share)
 		if err != nil { return uuid.UUID{}, errors.New("Error turning byte[] to Shared: " + err.Error()) }

		//Verify share integrity 
		temp, err = userlib.HMACEval(FileHKey, append(share.Giver, toBytes(share.Recipients)...))
		if err != nil {
			return uuid.UUID{}, errors.New("Error when creating HMac: " + err.Error())
		}
		if !userlib.HMACEqual(temp, share.Verification) {
			return uuid.UUID{}, errors.New("Share struct verification problem")
		}
	}

	//Add hash of username to Recipients after encrypting with Owner's Public Key 3
	//Design flaw here due to hackers being able to see who recipients are???
	temp, err = userlib.PKEEnc(ownerPub3, []byte(recUserhash))
	if err != nil { return uuid.UUID{}, errors.New("Problem with enc with Pub3:" + err.Error()) }

	share.Recipients = append(share.Recipients, temp)

	temp, err = userlib.HMACEval(FileHKey, append(share.Giver, toBytes(share.Recipients)...))
	if err != nil {
		return uuid.UUID{}, errors.New("Error when creating HMac: " + err.Error())
	}
	share.Verification = temp

	addData(shareUUID, share)

	

	return newUUID, err
}

/*
//key: Randomly gen value
Verification []byte
	//(Hash of sendername)
	Senderhash []byte
	//(Hash of name of file given by owner)
	OwnerFileH []byte
	//(Hash of owner name of file)
	OwnerNameH []byte

	FileSymKey []byte
	FileHKey []byte
*/

func (userdata *User) AcceptInvitation(senderUsername string, invitationPtr uuid.UUID, filename string) error {
	//Accepts the secure file share invitation created by senderUsername and located at invitationPtr in Datastore by giving the corresponding file a name of filename in the caller’s personal namespace.

	//After this function returns successfully, the caller should be able to take the actions detailed in Sharing and Revoking. Note that the first parameter to the StoreFile(), LoadFile(), and AppendToFile() functions in the client API is a filename in the caller’s personal namespace; accepting the invitation allows the caller to choose a name for the shared file in their personal namespace.

	//After this function returns successfully, the given invitation is considered accepted. Calling this function on an invitation that is already accepted is undefined behavior and will not be tested, which means your client can handle that scenario in any way that you feel makes sense.

	//Returns an error if:

	//X The caller already has a file with the given filename in their personal file namespace.
	//X The caller is unable to verify that the secure file share invitation pointed to by the given invitationPtr was created by senderUsername.
	//X The invitation is no longer valid due to revocation.
	//XThe caller is unable to verify the integrity of the secure file share invitation pointed to by the given invitationPtr.



	//What needs to be done here: delete Invite object, add entry to userData(recipient)'s Files.
	//Also update File.Access File.Verification, File.Tree
	//Update User.Check2 too
	//No need to delete File.Invitation array stuff. Do that during Revoke. Won't be used until then anyways.
	
	//update userdata (added something to File or something)
	err := userdata.UpdateUser()
	if err != nil { return err }


	//Note: if someone other than intended recipient tries to access file, they won't be able to decode anything, so error. If they try to modify information here, still error because can't be decoded???
	//Anyways Signature will verify tampering. 
	keydata, err := getKeyRing(userdata.keyring)
	if err != nil { return err }
	Priv2 := keydata.Priv2


	temp, err := getData(invitationPtr)
	//Can't access Invite Value
	//THIS WILL ERROR IF REVOKED BECAUSE INVITE OBJ IS GONE
	if err != nil { return errors.New("USER REVOKED or " + err.Error()) }

	var invitation Invite
	err = json.Unmarshal(temp, &invitation)
	//Can't unpack byte array into actual Invite
	if err != nil { return err }


	//Verifying sender (not necessarily owner of file shares)
	//Check if hash of sender in file is same as senderUsername
	Senderhash, err := userlib.PKEDec(Priv2, invitation.Senderhash)
	if err != nil { return err }
	
	//Find sender's (userHash + "Inv") public key
	senderInvVerif, err := getKey(string(Senderhash) + "Inv")
	if err != nil { return err }

	//Use DSVerifyKey to verify Invitation (DSVerify err is wrong)
	//VERIFY NOTHING IS CHANGED BEFORE WHETHER USER IS CHANGED
	err = userlib.DSVerify(senderInvVerif, appendFive(invitation.Senderhash, invitation.OwnerFileH, invitation.OwnerNameH, invitation.FileSymKey, invitation.FileHKey), invitation.Verification)
	if err != nil { return err }
	//Check if senderUserName input is correct.
	if !userlib.HMACEqual(Senderhash, userlib.Hash([]byte(senderUsername))) {
		return errors.New("MISMATCHING SENDER SOMEHOW (are you sure you put in the correct sendername?)")
	}


	newFilename := userlib.Hash([]byte(filename))
	//Search through user files to see if filename already taken
	for _, i := range userdata.Files {
		//(Hash of name of file in personal namespace) + (Hash of name of file given by owner) + (Hash of owner name of file) + (Sym Key (for FilePointer&File) + Hmac Key (For File verification))
		//64 + 64 + 64 + 16 + 16
		if userlib.HMACEqual(i[:64], newFilename) {
			return errors.New("File with same name already exists locally ("+filename+")")
		}
	}


	OwnerNameH, err := userlib.PKEDec(Priv2, invitation.OwnerNameH)
	if err != nil { return err }
	OwnerFileH, err := userlib.PKEDec(Priv2, invitation.OwnerFileH)
	if err != nil { return err }
	FileSymKey, err := userlib.PKEDec(Priv2, invitation.FileSymKey)
	if err != nil { return err }
	FileHKey, err := userlib.PKEDec(Priv2, invitation.FileHKey)
	if err != nil { return err }


	//Add entry inside User.Files
	//	- (Hash of name of "filename") + (Hash of name of file given by owner) + (Hash of owner name of file) + (Sym Key (for FilePointer&File) + Hmac KEY (For File verification))
	Pub1, err := getKey(userdata.userhash)
	//Can't unpack byte array into actual Keyring
	if err != nil { return err }

	//User Stuff
	newKey := appendThree(newFilename, OwnerFileH, OwnerNameH)
	//Value : (Sym Key (for FilePointer) + Hmac Key (For File verification))
	newValue, err := userlib.PKEEnc(Pub1, append(FileSymKey, FileHKey...))
	if err != nil { return err }
	//Appending []byte to [][]byte
	userdata.Files = append(userdata.Files, append(newKey, newValue...))

	addData(UUIDerr([]byte(userdata.userhash)), userdata)



	//Go to File struct
	file, err, _, _, fileLoc := getFileStruct(userdata.Files, keydata.Priv1, userlib.Hash([]byte(filename)), []byte(userdata.userhash))
	//4 situations: (noerr, nil), (err, nil) error, (noerr, so), *(err, so)*
	if fileLoc == nil {
		if err != nil {
			//Error with accessing or something
			return err
		}
		return errors.New("File being shared (" + filename + ") does not exist in the personal file namespace of the caller.")
	} else if err == nil {
		//Somehow person is already in File.Access
		return errors.New("Shouldn't have complete access to file (" + filename + ") yet.")
	}
	//	- delete Invite XXX <- don't need to do this, do in Revoke
	//	- Add userdata.userhash to File.Access after encrypting with fileSymKey
	//  - Why encrypt with SymKey? B/C Need to verify if has Access
	//		- Someone other than use needs to be able to decrypt
	file.Access = append(file.Access, userlib.SymEnc(FileSymKey, userlib.RandomBytes(16), []byte(userdata.userhash)))

	//generate new HMAC for Shards, SymKey, and Access
	temp, err = userlib.HMACEval(FileHKey, appendFour(toBytes(file.Access), file.SymKey, toBytes(file.Shards), toBytes(file.Invitations)))
	if err != nil {
		return errors.New("Error when creating HMac: " + err.Error())
	}

	file.Verification = temp
	//save File
	addData(UUIDerr(fileLoc), file)


	//Delete Invite object pointed to by UUID
	userlib.DatastoreDelete(invitationPtr)
	
	return nil
}

/*
Shared struct {
	//List of Hashes of user usernames that local User is sharing file with, asymmetrically encrypted through both User and Owner’s Public Key #3s. 
	Verification viia Hmac
	//Encrypt via owner Pub3
	Giver []byte
	Recipients [][]byte
*/

func (userdata *User) RevokeAccess(filename string, recipientUsername string) error {
	//Given a filename in the personal namespace of the caller, this function revokes access to the corresponding file from recipientUsername and any other users with whom recipientUsernamehas shared the file.

	//A revoked user must lose access to the corresponding file regardless of whether their invitation state is created or accepted.

	//The client MUST prevent any revoked user from using the client API to take any action on the file. However, recall from Threat Model that a revoked user may become malicious and use the Datastore API directly (see Sharing and Revoking).

	//After revocation, the client MUST return an error if the revoked user attempts to take action through the Client API on the file, with one exception: the case in which a user calls StoreFile on a file that has been revoked is undefined behavior and will not be tested.

	//*******
	//You may assume this function will only be called by the file owner on recipients with whom they directly shared the file (see Sharing and Revoking).
	//*******

	//Returns an error if:

	//>>>>  The given filename does not exist in the caller’s personal file namespace.
	//The given filename is not currently shared with recipientUsername.
	//Revocation cannot complete due to malicious action.

	//0--=p0-0o-p900ii

	//What needs to happen: 

	//Verify User has access to File + is owner
	keydata, err := getKeyRing(userdata.keyring)
	if err != nil { return err }
	Priv1 := keydata.Priv1

	err = userdata.UpdateUser()
	if err != nil { return err }

	var KeyHashes []byte 
	var ValueHashes []byte

	var filePointer FilePointer
	var file File
	var FileSymKey []byte
	var FileHKey []byte
	var BitSymkey []byte

	var fileLoc []byte

	fileHash := userlib.Hash([]byte(filename))

	fileExists := false
	//[][]byte
	for _, array := range userdata.Files {
    	//(Hash of name of file in personal namespace) + (Hash of name of file given by owner) + (Hash of owner name of file) 

    	if len(array) != 448 {
    		return errors.New("Abnormal File entry length")
    	}

    	//Hashes are 64 bytes
    	KeyHashes = array[:192]

    	//Value : (Sym Key (for FilePointer) + Hmac Key (For File verification))
    	ValueHashes = array[192:]

    	//get name of stored file (personal namespace)
    	if userlib.HMACEqual(KeyHashes[:64], fileHash) {

    		//Same hash as Owner's name AND owner name is current name
    		if userlib.HMACEqual(KeyHashes[64:128], fileHash) && userlib.HMACEqual(KeyHashes[128:], []byte(userdata.userhash)) {
    			fileExists = true
    			break
    		}

    		return errors.New("Revoker is not owner of file")
    	}
	}
	if !fileExists {
		return errors.New("File being Revoked (" + filename + ") does not exist in the personal file namespace of the caller.")
	}

	//verify recipient Exists
	recUserhash := userlib.Hash([]byte(recipientUsername))
	_, err = getKey(string(recUserhash))
	if err != nil { return err }


	//Value : (Sym Key (for FilePointer) + Hmac Key (For File verification))
	//userlib.DebugMsg(fmt.Sprint(len(ValueHashes)))
    output, err := userlib.PKEDec(Priv1, ValueHashes)
    if err != nil { return err }

    FileSymKey = output[:16]
    FileHKey = output[16:]


    //Go to FilePointer
    //Address is hash of ((Hash of name of file given by owner) + (Hash of owner name of file))
    temp, err := getData(UUIDerr(userlib.Hash(KeyHashes[64:])))
	//Can't access FilePointer Address
	if err != nil { return err }

	err = json.Unmarshal(temp, &filePointer)
	//Can't unpack byte array into actual FilePointer
	if err != nil { return err }


	//Go to File
	//Decrypt FileLoc using symmetric key
	fileLoc = userlib.SymDec(FileSymKey, filePointer.FileLoc)
    temp, err = getData(UUIDerr(fileLoc))
	//Can't access File Address
	if err != nil { return err }

	err = json.Unmarshal(temp, &file)
	//Can't unpack byte array into actual File
	if err != nil { return err }

	//Verify Hmac for file
	temp, err = userlib.HMACEval(FileHKey, appendFour(toBytes(file.Access), file.SymKey, toBytes(file.Shards), toBytes(file.Invitations)))
	if err != nil {
		return errors.New("Error when creating HMac: " + err.Error())
	}
	if !userlib.HMACEqual(temp, file.Verification) {
		//userlib.DebugMsg(string(temp))
		//userlib.DebugMsg(string(file.Verification))
		return errors.New("File struct HMac verification failed")
	}

	bad := true
	//bad2 := true
	//Verify you should have access (Again)
	for _, i := range file.Access {
		//if !(bad || bad2) {
		//	break
		//}

		//userlib.DebugMsg(string(Userhash))
		dec := userlib.SymDec(FileSymKey, i)
		if userlib.HMACEqual([]byte(userdata.userhash), dec) {
			bad = false
			break
		}
		//If not accepted, then obviously they don't have access
		//This checks if they accepted invite. 
		//if userlib.HMACEqual(recUserhash, dec) {
		//	bad2 = false
		//}
	}
	if bad {
		return errors.New("Shouldn't have access to file")
	}
	//if bad2 {
	//	return errors.New(filename + " not currently shared with " + recipientUsername)
	//}

	//+=+=+><><><<><>><>><><><><><><><><<><><><><><><><><><><><><><><<><>>>>><><><>><><<><><><><<>

	//#######################
	//Removing Shared structs and names from file.Invitations and file.Access
	//#######################
	//Go through Shared tree


	//Problem: if person didn't Share then they don't have Share struct
	//shareUUID := UUIDerr(userlib.Hash(append(recUserhash, KeyHashes[64:]...)))
	//temp, err = getData(shareUUID)
	//if err != nil { 
		//Error: The given filename is not currently shared with recipientUsername.
	//	return errors.New("SHARE STRUCT FOR RECIPIENT DOESN'T EXIST")
	//}

	blackList, err := sharetreeIter(recUserhash, keydata.Priv3, FileHKey, KeyHashes[64:])
	if err != nil {
		return err
	}
	//userlib.DebugMsg(string(toBytes(blackList)))
	
	//Go through userHashList(from above):
	//	Go through Invite, decrypting with ownerPrivKey2
	//		//Value = (receiver hash + address to Invite struct)
	//		(remove entry with hashes that match, delete corresponding Invite struct)
	//	Error if no HMACEqual() found
	//
	//	Go through Access, decrypting through FileSymKey
	//		//Value = userhash
	//		//Note: even if someone is invited they might not have Access
	//		//It's actually fine if none is found here. 
	//		Delete element if exists
	//
	//	???????
	//	Go to corresponding User and delete entry???
	// ^ Don't need to I guess. getFileStruct will error if they try to access. (Later modify FilePointer)
	//Note: hashes in BuserHash are already decrypted (or should be)

	found := 0
	//len(file.Invitations) shouldn't be 0 or something.
	//If it is though there is no loop
	for i := 0; i <= len(file.Invitations) - 1; i++ {

		j := file.Invitations[i]

		//(receiver hash + address to Invite struct)
		inviteInfo, err := userlib.PKEDec(keydata.Priv2, j)
		if err != nil { 
			return errors.New("Problem with dec with Priv2:" + err.Error()) 
		}


		for _, BuserHash := range blackList {
			//if blacklistedhash matches a hash from Invitations, 
			if userlib.HMACEqual(inviteInfo[:64], BuserHash) {
				//delete from File.Invitations
				file.Invitations = remove(file.Invitations, i)
				//delete invite. If invite doesn't exist, good for them I guess. Less work
				userlib.DatastoreDelete(UUIDerr(inviteInfo[64:]))

				found++

				//This makes it so that we search through the same file.Invitation index again
				//Due to the nature of remove()
				i--

				//Should only find 1 match every Buser
				//Not having Invitations loop outside due to how array removal works

				break
			}
		}
	}
	//Amount of Invites deleted should correspond to number of BlackListed hashes identified
	if found != len(blackList) {
		//userlib.DebugMsg(fmt.Sprintf("%v", found))
		//userlib.DebugMsg(fmt.Sprintf("%v", len(blackList)))
		return errors.New("Somehow someone wasn't in File.Invitations, so they probably weren't invited")
	}

	//For reencryption
	newFileSymKey := userlib.RandomBytes(16)
	newFileHKey := userlib.RandomBytes(16)

	userEntry := append(newFileSymKey, newFileHKey...)

	for i := 0; i <= len(file.Access) - 1; i++ {
		//A hash of the user with Access
		decUserHash := userlib.SymDec(FileSymKey, file.Access[i])

		found = 0
		for _, BuserHash := range blackList {
			//if blacklistedhash matches a hash from Access, 
			if userlib.HMACEqual(BuserHash, decUserHash) {
				//delete from File.Access
				file.Access = remove(file.Access, i)

				//This makes it so that we search through the same file.Access index again
				//Due to the nature of remove()
				i--
				//Should only find 1 or 0 matches
				found = 1
				break
			} 
		}
		//if current Access entry doesn't match any on blacklist, then reencrypt that entry
		if found == 0 {
			file.Access[i] = userlib.SymEnc(newFileSymKey, userlib.RandomBytes(16), decUserHash)


			//#######################
			//updating User and Shared
			//#######################

			//Create new FilePointer File, FileAddon (addon just everything together)
				//You can just get the struct contents then save it to another UUID after modifying contents.
			//Store new file with complete file contents
			//Iterate through Access list (updated), 
			//	- go to User, update SymKey and HKey for relavent files [64:], encrypt SymKey and HKey with their Pub1 keys

			//	- The following should remain the same for the Users (don't need to generate new Verification variable)
			//		fileCheck := []byte{}
			//		for _, i := range userdata.Files {
			//			fileCheck = append(fileCheck, i[:192]...)
			//		}
			//	 	temp, err = userlib.HMACEval(keydata.FilesMacKey, append(userdata.Salt2, fileCheck...))

			//Some user with access
			var allowed User
			allowUUID := UUIDerr([]byte(decUserHash))
			val, err := getData(allowUUID)
			if err != nil { 
				return errors.New("Error getting User: " + err.Error()) 
			}
			err = json.Unmarshal(val, &allowed)
		 	if err != nil { 
		 		return errors.New("Error turning byte[] to User: " + err.Error())
		 	}

		 	Pub1, err := getKey(string(decUserHash))
		 	if err != nil { return err }

			bad = true
			for index, k := range allowed.Files {
				//(Hash of name of file in personal namespace) + (Hash of name of file given by owner) + (Hash of owner name of file)
				//key := k[:192]
				//(Sym Key (for FilePointer&File) + Hmac Key (For File verification)) <- needs updating
				//value := k[192:]
				if len(k) != 448 {
    				return errors.New("Abnormal File entry length")
    			}

				//The filename given by owner matches entry in User.Files
				if userlib.HMACEqual(fileHash, k[64:128]) {
					bad = false
					newEntry, err := userlib.PKEEnc(Pub1, userEntry)
					if err != nil { return err }

					//Update User.Files entry
					allowed.Files[index] = append(k[:192], newEntry...)
					addData(allowUUID, allowed)

					break
				}
			} 
			if bad {
				return errors.New("Somehow can't find file in Accepted individual") 
			}



			//Update Verification in Shared stuff (do along with User updates due to similar userhash)
			//For encrypting each user's new HMcKey and FileSymKey in User.Files
			//Pub1, err := getKey(userdata.userhash)

			var Share Shared
			shareUUID := UUIDerr(userlib.Hash(append(decUserHash, KeyHashes[64:]...)))
			temp, err := getData(shareUUID)
			if err != nil { 
				//Possibility that person didn't share with someone else, so no possible Shared struct
				continue
			}

			err = json.Unmarshal(temp, &Share)
			if err != nil { 
				return errors.New("Error turning byte[] to Shared: " + err.Error())
			}

			//sharetreeIter verified integrity of deleted Shares.
			//Now verify integrity of non-deleted ones.
			temp, err = userlib.HMACEval(FileHKey, append(Share.Giver, toBytes(Share.Recipients)...))
			if err != nil {
				return errors.New("Error when creating HMac: " + err.Error())
			}
			if !userlib.HMACEqual(temp, Share.Verification) {
				return errors.New("(nondeleted)Share struct verification problem")
			}

			//Don't need to modify Recipients: encrypted via Pub3
			Share.Verification, err = userlib.HMACEval(newFileHKey, append(Share.Giver, toBytes(Share.Recipients)...))
			if err != nil {
				return errors.New("Error when creating HMac: " + err.Error())
			}

			addData(shareUUID, Share)

		}
	}


	//+=+=+><><><<><>><>><><><><><><><><<><><><><><><><><><><><><><><<><>>>>><><><>><><<><><><><<>

	//#######################
	//Obtaining File contents
	//#######################

	var content []byte
	BitSymkey = userlib.SymDec(FileSymKey, file.SymKey)
	//Decrypt address of each FileAddon, access Addon, verify Addon contents, append Addon contents to result
	for _, i := range file.Shards {
		
		addonUUID := UUIDerr(userlib.SymDec(BitSymkey, i))
		temp, err := getData(addonUUID)
		//Can't access FileAddon Value
		if err != nil { 
			//userlib.DebugMsg(string(toBytes(file.Shards)))
			return err 
		}

		var fileBit FileAddon
		err = json.Unmarshal(temp, &fileBit)
		//Can't unpack byte array into actual FileAddon
		if err != nil { return err }

		temp, err = userlib.HMACEval(FileHKey, fileBit.Contents)
		if err != nil {
			return errors.New("Error when creating HMac: " + err.Error())
		}
		if !userlib.HMACEqual(fileBit.Verification, temp) {
			return errors.New("HMac Verification for FileAddon Fail: " + err.Error())
		}

		//Add decrypted contents to result array
		content = append(content, userlib.SymDec(BitSymkey, fileBit.Contents)...)

		//Delete FileAddon struct
		userlib.DatastoreDelete(addonUUID)
	}

	//+=+=+><><><<><>><>><><><><><><><><<><><><><><><><><><><><><><><<><>>>>><><><>><><<><><><><<>

	//#######################
	//updating FilePointer, File, FileAddon
	//#######################


	//Gen new Symkeys and HKey
	newfileLoc := userlib.RandomBytes(16)

	newBitSymkey := userlib.RandomBytes(16)
	//Get contents of file decrypted (loadFile)
	newfileBitLoc := userlib.RandomBytes(16)


	//Creating new FileAddon

	var fileBit FileAddon
	fileBit.Contents = userlib.SymEnc(newBitSymkey, userlib.RandomBytes(16), content)

	//Hmac for file contents, FileHKey from User
	temp, err = userlib.HMACEval(newFileHKey, fileBit.Contents)
	fileBit.Verification = temp
	if err != nil {
		return errors.New("Error when creating HMac: " + err.Error())
	}
	//Save fileAddon
	addData(UUIDerr(newfileBitLoc), fileBit)



	//Updating file

	//file.Access alreay updated with newFileSymKey
	//file.Invitations is encrypted via Owner's PublicKey2. Doesn't need to be updated.
	file.SymKey = userlib.SymEnc(newFileSymKey, userlib.RandomBytes(16), newBitSymkey)
	//shard contents are UUID of FileAddon encrypted via BitSymkey
	file.Shards = [][]byte{userlib.SymEnc(newBitSymkey, userlib.RandomBytes(16), newfileBitLoc)}

	temp, err = userlib.HMACEval(newFileHKey, appendFour(toBytes(file.Access), file.SymKey, toBytes(file.Shards), toBytes(file.Invitations)))
	if err != nil {
		return errors.New("Error when creating HMac: " + err.Error())
	}
	file.Verification = temp

	//Upload new file, delete old file. Reminder that old Addons were already deleted
	addData(UUIDerr(newfileLoc), file)
	userlib.DatastoreDelete(UUIDerr(fileLoc))


	//Update filePointer

	filePointer.FileLoc = userlib.SymEnc(newFileSymKey, userlib.RandomBytes(16), newfileLoc)
	//Hash of (fileHash + Hash of owner name of file)
	addData(UUIDerr(userlib.Hash(KeyHashes[64:])), filePointer)

	return nil
}

	//	- Look through Shared.Recipient, decrypt with owner(user) Priv3
	//	- Use Recipient hashes to get other structs
	//	- Create list of userHashes to delete while deleting Shared along the way
func sharetreeIter(userHash []byte, Priv3 userlib.PKEDecKey, FileHKey []byte, ID []byte) (blackList [][]byte, err error) {

	var Share Shared
	shareUUID := UUIDerr(userlib.Hash(append(userHash, ID...)))
	temp, err := getData(shareUUID)
	if err != nil { 
		//Possibility that person didn't share with someone else, so no possible Shared struct
		return [][]byte{userHash}, nil
	}

	err = json.Unmarshal(temp, &Share)
	 if err != nil { 
	 	return nil, errors.New("Error turning byte[] to Shared: " + err.Error())
	 }

	//Verify share integrity 
	temp, err = userlib.HMACEval(FileHKey, append(Share.Giver, toBytes(Share.Recipients)...))
	if err != nil { 
		return nil, errors.New("Error when creating HMac: " + err.Error()) 
	}
	if !userlib.HMACEqual(temp, Share.Verification) {
		return nil, errors.New("rootShare struct verification problem")
	}

	SenderHash, err := userlib.PKEDec(Priv3, Share.Giver)
	if err != nil { 
		return nil, errors.New("Problem with dec with Priv3:" + err.Error()) 
	}
	if !userlib.HMACEqual(userHash, SenderHash) {
		return nil, errors.New("Name mismatch somehow")
	}

	blackList = [][]byte{userHash}

	for _, i := range Share.Recipients {
		DecryptUserHash, err := userlib.PKEDec(Priv3, i)
		if err != nil { 
			return nil, errors.New("Problem with dec with Priv3:" + err.Error()) 
		}

		temp, err := sharetreeIter(DecryptUserHash, Priv3, FileHKey, ID)
		if err != nil {
			return nil, err
		}
		blackList = append(blackList, temp...)
	}
	userlib.DatastoreDelete(shareUUID)

	return blackList, nil
}

//remove element at index i from s
func remove(s [][]byte, i int) [][]byte {
    s[i] = s[len(s)-1]
    return s[:len(s)-1]
}

