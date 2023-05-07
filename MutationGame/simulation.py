import numpy as np
from bearlibterminal import terminal
from random import choices
import inputLists as dicts
import sys

#Each virus created reduces a cell's LIFESPAN by 1

#current set genome for new cells
cellG: np.array = None
virusG: np.array = None

cooldown = 5.0

#Last time cellG was updated. Compared to cooldown.
lastUpdated = None

genomeLen = 10
mutationRate = 0.05

#Printing out the starting menu
def menu():
	terminal.clear()
	#Generated via https://fsymbols.com/text-art/
	terminal.printf(2, 1, "███╗░░░███╗██╗░░░██╗████████╗░█████╗░████████╗██╗░█████╗░███╗░░██╗")
	terminal.printf(2, 2, "████╗░████║██║░░░██║╚══██╔══╝██╔══██╗╚══██╔══╝██║██╔══██╗████╗░██║")
	terminal.printf(2, 3, "██╔████╔██║██║░░░██║░░░██║░░░███████║░░░██║░░░██║██║░░██║██╔██╗██║")
	terminal.printf(2, 4, "██║╚██╔╝██║██║░░░██║░░░██║░░░██╔══██║░░░██║░░░██║██║░░██║██║╚████║")
	terminal.printf(2, 5, "██║░╚═╝░██║╚██████╔╝░░░██║░░░██║░░██║░░░██║░░░██║╚█████╔╝██║░╚███║")
	terminal.printf(2, 6, "╚═╝░░░░░╚═╝░╚═════╝░░░░╚═╝░░░╚═╝░░╚═╝░░░╚═╝░░░╚═╝░╚════╝░╚═╝░░╚══╝")
	terminal.printf(29, 8, "Version 0.1")

	terminal.printf(21, 20, "Press [bkcolor=gray][color=blue][[SPACE]][/color][/bkcolor] to start game")
	terminal.printf(23, 21, "Press [color=red][[Q]][/color] to quit game")

	terminal.refresh()

def infectionChance(virusG: np.array, cellG: np.array):
	#https://stackoverflow.com/questions/31093529/minimum-distance-between-two-elements-of-a-circular-list
	#print(np.vectorize(dicts.charConv.get)(cellG))
	#print(np.vectorize(dicts.charConv.get)(virusG))

	dist = abs(virusG - cellG)
	#closeness (score) = total distance - minimum distance
	result = (24 - np.minimum(48 - dist, dist))/24
	#result = sum((cellG - virusG)%24)/20

	#print(str(result))
	result = sum(result)/genomeLen

	return result


#Generates a new viral genome based on mutation rate variable
#Every part of the genome has [mutationRate] of changing to some random value
#https://stackoverflow.com/questions/65256411/whats-the-fastest-way-of-finding-a-random-index-in-a-python-list-a-large-numbe
#(virusG: np.array)->np.array
def mutate():
	global virusG

	#As the link above mentioned, random.sample is fastest for <50 elements
	#https://stackoverflow.com/questions/14992521/python-weighted-random
	#HOWEVER sample() doesn't allow for replacement so it's not ideal
	#Solution is to use random.choices instead of np.random.choices 
	mask = np.array(choices([0,1], weights=[1-mutationRate, mutationRate], k=genomeLen))
	newGenome = np.array(choices(range(1,49), k=genomeLen))
	newGenome = mask*newGenome

	newGenome[newGenome == 0] = virusG[newGenome == 0]
	
	terminal.printf(2, 15, "[color=crimson]Virus Genome:[/color]   (Mutation Rate: " + "{:.1%}".format(mutationRate) + ")")

	PLetter = np.vectorize(dicts.charConv.get)(newGenome)
	for i in range(genomeLen):
		letter = PLetter[i]
		#Special processing due to how bearlibterminal prints the following characters
		if letter == "]":
			letter = "]]"
		if letter == "[":
			letter = "[["

		if  mask[i]:
			terminal.printf(3+i, 16, "[bkcolor=yellow][color=red]"+letter+"[/color][/bkcolor]")
		else:
			#Need to color background back to original because otherwise the purple coloring "sticks"
			terminal.printf(3+i, 16, "[bkcolor=black]"+letter+"[/bkcolor]")
	
	virusG = newGenome

	#return newGenome


#Sets up variables like starting viral and cell genomes
def setup():
	#Setting up length of Genome
	intstring = ""
	while True:
		setupPrint1(intstring)
		A = terminal.read()
		if A == terminal.TK_CLOSE:
			terminal.close()
			sys.exit()
			break

		#inputting numbers
		if A in dicts.numConv.keys() and 27 <= dicts.numConv[A] <= 36:
			if intstring == "0" :
				intstring = dicts.charConv[dicts.numConv[A]]
				continue
			intstring += dicts.charConv[dicts.numConv[A]]
		elif A == terminal.TK_ENTER:
			if intstring == "" or intstring == "0":
				continue

			global genomeLen
			genomeLen = int(intstring)
			break
		elif A == terminal.TK_BACKSPACE:
			intstring = intstring[:-1]
		pass

	#Setting up mutation rate
	intstring = ""
	while True:
		setupPrint2(genomeLen, intstring)
		A = terminal.read()
		if A == terminal.TK_CLOSE:
			terminal.close()
			sys.exit()
			break

		#inputting numbers
		if A in dicts.numConv.keys() and 27 <= dicts.numConv[A] <= 36:
			if intstring == "0" :
				intstring = dicts.charConv[dicts.numConv[A]]
				continue
			intstring += dicts.charConv[dicts.numConv[A]]
		elif A == terminal.TK_ENTER:
			if intstring == "" or int(intstring) > 100:
				continue

			global mutationRate
			mutationRate = int(intstring)/100
			break
		elif A == terminal.TK_BACKSPACE:
			intstring = intstring[:-1]
		pass

	temp = range(1,49)
	global cellG
	cellG = np.array(choices(temp, k=genomeLen))
	global virusG
	virusG = np.array(choices(temp, k=genomeLen))

	chance = infectionChance(virusG, cellG)
	while 0.4 < chance and chance > 0.5:
		#print(str(chance))
		virusG = np.array(choices(temp, k=genomeLen))
		chance = infectionChance(virusG, cellG)


	#print("Starting infection chance:", str(chance))

def setupPrint1(intstring):
	terminal.clear()
	terminal.printf(30, 8, "[color=cyan]Loading...[/color]")
	terminal.printf(2, 11, "Please set the length of the cell/viral genome.")
	terminal.printf(2, 12, "Press [[ENTER]] to submit. (Please select a length > 0)")
	terminal.printf(2, 13, "Note: Suggested length is 10.")
	terminal.printf(3, 14, str(intstring))
	terminal.refresh()

def setupPrint2(intstring, mutationRate):
	setupPrint1(intstring)
	terminal.printf(2, 16, "Please set the rate at which the virus mutates.")
	terminal.printf(2, 17, "Each part of the virus (set in previous option) will have this")
	terminal.printf(2, 18, "probability to mutate every turn.")
	terminal.printf(2, 19, "Press [[ENTER]] to submit. (Please select a rate < 101%)")
	terminal.printf(2, 20, "Note: Suggested rate is 5%, or an input of 5.")
	terminal.printf(3, 21, str(mutationRate))
	terminal.refresh()


def game():
	setup()
	terminal.clear()

	A = 1
	turns = 0
	index = 0
	while A != terminal.TK_CLOSE:
		#exit game
		if A == terminal.TK_TAB:
			menu()
			return
		elif A == terminal.TK_LEFT and index > 0:
			index -= 1
		elif A == terminal.TK_RIGHT and index < genomeLen-1:
			index += 1
		elif A in dicts.numConv.keys():
			#Modify Cell genome is valid key is pressed
			#global cellG
			cellG[index] = dicts.numConv[A]

		terminal.printf(28, 2, "Turns Taken: " + str(turns))

		#printing out Player genome
		terminal.printf(2, 18, "[color=sky]Cell Genome:[/color]    (Move via ← or → arrow keys)")
		PLetter = np.vectorize(dicts.charConv.get)(cellG)
		for i in range(genomeLen):
			letter = PLetter[i]
			#Special processing due to how bearlibterminal prints the following characters
			if letter == "]":
				letter = "]]"
			if letter == "[":
				letter = "[["

			if index == i:
				terminal.printf(3+i, 19, "[bkcolor=purple][color=green]"+letter+"[/color][/bkcolor]")
			else:
				#Need to color background back to original because otherwise the purple coloring "sticks"
				terminal.printf(3+i, 19, "[bkcolor=black]"+letter+"[/bkcolor]")


		terminal.printf(2, 4, "Help your B-Cell create the best antibody against the virus!")
		terminal.printf(2, 5, "[color=yellow]Win by obtaining an Attachment Rate > 85%![/color]")

		terminal.printf(2, 7, "TLDR: Match the cell genome with the virus genome. Use the arrow keys")
		terminal.printf(2, 8, "to select what place to change and the keyboard for what to change")
		terminal.printf(2, 9, "that place to. [bkcolor=yellow][color=red]Highlighted[/color][/bkcolor] virus places are regions of the virus")
		terminal.printf(2, 10, "that mutated from the previous turn.")

		if (infectionChance(virusG, cellG) > 0.85):
			win(turns, str("{:.5%}".format(infectionChance(virusG, cellG))))
			return


		terminal.printf(2, 12, "Attachment Rate: " + str("{:.5%}".format(infectionChance(virusG, cellG))))

		mutate()

		terminal.printf(2, 23, "Press [[TAB]] to return to Main Menu")

		terminal.refresh()
		turns += 1
		A = terminal.read()
		pass

	#TK_CLOSE was pressed
	terminal.close()

def win(turns, attatchment):

	terminal.clear()

	terminal.printf(27, 5, "[bkcolor=purple][color=yellow]★★★YOU WIN!!!★★★[/color][/bkcolor]")


	terminal.printf(28, 8, "[color=amber]Turns Taken: " + str(turns) + "[/color]")

	terminal.printf(18, 9, "[color=amber]Final Attachment Rate: " + attatchment + "[/color]")

	terminal.printf(2, 21, "Press [[TAB]] to return to Main Menu")
	terminal.refresh()

	A = 0
	while A != terminal.TK_CLOSE:
		#exit game
		if A == terminal.TK_TAB:
			menu()
			return
		A = terminal.read()

	terminal.close()



