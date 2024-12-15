To compile the project, install Maven and run 
```
mvn compile
```
We have not constructed a nice user interface, so the program is currently hardcoded to run the tests. 

To run your own Datalog program, add it to `src/test`, navigate to `src/main/java/Main.java`, delete lines 8-9, and change the string on line 11 to the name of your program. 

To change the solver, comment out the current solver and uncomment the solver you want (lines 22-25).

To run naive evaluation, uncomment line 26 and comment out line 27.
