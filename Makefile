# This is the simple makefile for the chatserve and chatclient programs 
# Type "make" to compile the java file to bytecode classes
# Type "make clean" to clean the directory by deleting the compiled class files
chatserve: chatserve.java
	javac chatserve.java

clean:
	$(RM) *.class