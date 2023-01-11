# fileSharer

Coded in goLang.

Functions: 

InitUser: Initializes User struct given a username and password input
GetUser: Returns an initialized User struct given a username and password

User.StoreFile: Stores file contents into a dataStore server
User.LoadFile: Retrieves and returns file contents from dataStore server
User.AppendToFile: Adds contents to end of stored file in dataStore serever
User.CreateInvitation: Invites another User to access/edit a file
User.AcceptInvitation: Accepts invitation to access/edit a file
User.RevokeAccess: Revokes a user's access to a file. 
