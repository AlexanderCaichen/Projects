import random
import sys

###########################################################################
######################### TO RUN (MAC terminal)############################
#cd to file location (Ex: cd Desktop)
#Functions available to run are "encoder" (takes in 2 arguments) and "decoder" (takes in 1 argument)
#python3 (function) (arg1) (arg2 if applicable)

#Results should be printed in terminal
###########################################################################



invert={'z': 0,'y': 25,'x': 24,'w': 23,'v': 22,'u': 21,'t' :20 ,'s' :19 ,'r' :18 ,'q' :17 ,'p' :16 ,'o' :15 ,'n' :14 ,'m' :13 ,'l' :12 ,'k' :11 ,'j' :10 ,'i' :9 ,'h' :8 ,'g' :7 ,'f' :6 ,'e' :5 ,'d' :4 ,'c' :3 ,'b' :2 ,'a' :1}
dictionary={1: 'a', 2: 'b', 3: 'c', 4: 'd', 5: 'e', 6: 'f', 7: 'g', 8: 'h', 9: 'i', 10: 'j', 11: 'k', 12: 'l', 13: 'm', 14: 'n', 15: 'o', 16: 'p', 17: 'q', 18: 'r', 19: 's', 20: 't', 21: 'u', 22: 'v', 23: 'w', 24: 'x', 25: 'y', 0: 'z'}
no=[","," ","'",":",'"',".",";","!","-","–","?","%","=","+"]

#Each variation of Caesar cipher is length "length" and separated by a number
def encoder(sentence, length): #sentence is a string, returns cipher in terms of "ber". Need to run two times.
    #cleaning
    sentence=sentence.lower()
    for i in no:
        sentence=sentence.replace(i, "")
    #separate into list
    listy=separator(sentence, int(length))
    #building
    sentence2=""
    for part in listy:
        maxer=int("9"*len(part))
        if len(str(maxer))<2:
            maxer=99
        ber=random.SystemRandom().randint(0, maxer) #or random.randrange(1,9999, 47)
        sentence2+=str(ber)
        for i in part:
            num=invert[i]
            sentence2+=dictionary[(int(num)+ber)%26] #Note 26%26 =0
    print(sentence2)
    return sentence2

#######################################################
def decoder(message):
    #find number, shorten sentence/string in process (from left side).
    #when type is not an integer (a letter), shorten and save the letters until the next value is an integer
    #take number and decode numbers with it
    #repeat until nothing left in sentence
    
    #just in case:
    message=message.lower()
    for i in no:
        message=message.replace(i, "")
    decoded=[]
    while len(message) > 0:
        number=""
        section=""
        while message[0] in "1234567890":
            number+=message[0]
            message=message[1:]
        key=int(number)%26
        while len(message)>0 and (message[0] not in "1234567890"):
            decode_num=invert[message[0]]-key #reversing positional changes
            if decode_num<0:
                decode_num+=26
            section+=dictionary[decode_num]
            message=message[1:]
        decoded.append(section)
    temp = "".join(decoded)
    print(temp)
    return temp
    
def separator(mess, portion_size): #returns list of numbers of portion_size (except last one)
    mister=[]
    for i in range(0, len(mess), portion_size):
        mister.append(mess[i:i+portion_size])
    return mister
    
    

#test things here
#python3 Encoder.py
#print(encoder("I really don't like where this is going!?.'", 7), type(encoder("I really don't like where this is going!?.'", 7)))
#print(separator("asdfasd", 2),"".join(separator("asdfasd", 2))) #['as', 'df', 'as', 'd'] asdfasd
#a= encoder("JACK ate a fiery pepper", 2)
#print(a)
#print(decoder(a))
#print(decoder("2Cukh-2rfcq12wuqe17bzvj12dlyl89aepo38eyaf16uhut-1joui8me76exiqqv18ykgx15igxa24jgml"))

if __name__ == '__main__':
    n = len(sys.argv)
    #print(n)
    #print(sys.argv)
    if n==3:
        globals()[sys.argv[1]](sys.argv[2])
    elif n==4:
        globals()[sys.argv[1]](sys.argv[2], sys.argv[3])
    else:
        print("wrong argument number")
