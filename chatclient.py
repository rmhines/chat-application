'''
Project 1: Client-Server Network Application (Chat Program)

Programmer: Bobby Hines

Due Date: November 1st, 2015

CS372-400, Fall 2015       Instructor: Stephen Redfield 

Description:
This is the "client" side of the chat program, although it has 
a lot in common with the "server" program. Notable differences
are that this program must be initiated after the server program 
has started, and it does not rely on explicit background 
threading to facilitate concurrent messaging. This client app 
runs until the connection is interrupted, or the user enters the
"\quit" command. 

Input: The user must specify a host and a port number in order 
to start the client app. These are passed as command line 
arguments. It also takes in any messages typed from this end 
and sends them to the client socket. 

Output: The only output are messages received from the server 
(displayed through this program) and messages sent to server 
peers (displayed through the server console). 

'''

#!/usr/bin/python

# Client
import socket, select, string, sys

# This displays the input prompt along with the client's desired handle
def prompt(handle) :
    sys.stdout.write(handle + '> ')
    sys.stdout.flush()
 
#main function
if __name__ == "__main__":

    # Check for the correct number of args
    if(len(sys.argv) < 3) :
        print 'Usage : python chatclient.py hostname port'
        sys.exit()

    # Save the host name as a string
    host = sys.argv[1]

    # Save the port, parsed as an integer
    port = int(sys.argv[2])
    
    # Open a TCP socket
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(2)
     
    # connect to remote host
    try :
        s.connect((host, port))
    except :
        print 'Unable to connect with host' + host + ' on port ' + str(port) + '.'
        sys.exit()

    # Get the user's desired handle
    print 'Please enter a handle: '
    handle = sys.stdin.readline()

    while (len(handle) < 1 | len(handle) > 10):
    	print 'Sorry, your handle must be 1-10 characters: '
    	sys.stdin.readline()

    # Strip any sneakily added newlines/null terminators/whitespace
    handle = handle.rstrip()

    # Now that we have a valid handle, notify user of successful connection
    print 'Connection established with ' + host + ':' + str(port) + '.\n'

    # Display the prompt and await some message input
    prompt(handle)
    
    # Listen for messages from either end until connection lost or "\quit" command
    while 1:
        socket_list = [sys.stdin, s]
         
        # Get the list of sockets we can interact with
        read_sockets, write_sockets, error_sockets = select.select(socket_list , [], [])
        
        # Send to any listening sockets, in this case just the server
        for sock in read_sockets:
            # Accept incoming messages from the server
            if sock == s:
            	# Set a large buffer to accept long messages
                data = sock.recv(4096)
                # If whatever was received was falsy, the connection was interrupted
                if not data :
                    print '\nDisconnected from chat server'
                    sys.exit()
                else :
                    # Print the received message
                    sys.stdout.write('(incoming message)\n')
                    sys.stdout.write(data)
                    prompt(handle)
             
            else :
            	# Socket activity was on this end, and we need to send the client's message
                msg = sys.stdin.readline()
                # Check if the input was the "\quit" command
                if (msg.rstrip() == '\\quit'):
                	sys.exit()

               	# Send the message through the socket to the server side
                s.send(handle + '> ' + msg)
                prompt(handle)
