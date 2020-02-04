# Grocery-Bagging

Overview:

This project develops an algorithm to bag groceries that have constraints on what items can be bagged with what, how much you can put in a bag, and the number of bags available. The program is given the name of a text file that defines the grocery bagging problem as the last command line argument. The first line in the file determines the number of bags available. The next line determines the maximum bag size. The remaining lines define the items that must be bagged. Each line has a distinct name, a number associated with it which defines its size, possibly followed by a + or a – which represents compatibility (aka positive compatibility) or incompatibility (aka negative compatibility) and followed by a list of items. The + or – and a list of items define the constraints on what can be bagged with the item. A + indicates that it is OK to bag the item with anything in the list that follows (i.e. positive compatibility), but nothing else. A – indicates that it is NOT OK to bag the item with anything in the list (i.e. negative compatibility), but you can bag the item with anything else. If no sign is given and no list is provided for the item on a given line, then the item is assumed to be compatible with everything in the problem. However, the above item can still be rejected by some other item(s) in the problem. In either case, the program will determine at least one way to bag all the items and at the same time satisfy the given constraints.

Run Configurations in Eclipse IDE:

test_1000_plus_reasonable.txt   -breadth


