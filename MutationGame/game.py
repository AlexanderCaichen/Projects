from bearlibterminal import terminal
import simulation as sim


def set_config(terminal):
    terminal.set("input:"
    			#, mouse+
                "filter=[keyboard];"
                #"precise-mouse=true;"
                "window:"
                "resizeable=false,"
                "title='Mutation',"
                #X x Y
                "size=70x25;"
                )

def hex_to_string(hex):
    if hex[:2] == '0x':
        hex = hex[2:]
    string_value = bytes.fromhex(hex).decode('utf-8')
    return string_value


terminal.open()
set_config(terminal)

#terminal.printf(2, 1, "Hello, world!")
#print main menu
sim.menu()

A = 1
while A != terminal.TK_CLOSE and A != terminal.TK_Q:
	#print("2");
	"""if A == "":
		terminal.printf(2, 2, hex(A))
		terminal.printf(3, 3, str(A))
	"""
		
	if A == terminal.TK_SPACE:
		#run game
		sim.game()
	#terminal.refresh()
	A = terminal.read()
	pass

terminal.close()
