#Sample Scenario

#java node 0 4 "A message from 0 to 4" 40 &> output1.txt &
#java node 1 1 &> output2.txt &
#java node 2 2 &> output3.txt &
#java node 3 3 &> output4.txt &
#java node 4 0 "Message from 4 to 0" 80 &> output5.txt &
#java node 5 5 &> output6.txt &
#java controller &> output7.txt &

#Scenario 1
java node 0 1 "message from 0" 50 &> output1.txt &
java node 1 1 &> output2.txt &
java node 2 2 &> output3.txt &
java node 3 2 &> output4.txt "message from 3" 50 &
java node 4 4 &> output5.txt &
java node 6 6 &> output6.txt &
java node 7 7 &> output7.txt &
java node 8 8 &> output8.txt &
java node 9 2 "message from 9" 25 &> output9.txt &
java controller &> output10.txt &



#Scenario 2
java node 0 1 "message from 0" 50 &> output1.txt &
java node 1 1 &> output2.txt &
java node 2 2 &> output3.txt &
java node 3 2 "message from 3" 100 &> output4.txt &
java node 4 4 &> output5.txt &
java node 6 6 &> output6.txt &
java node 7 7 &> output7.txt &
java node 8 8 &> output8.txt &
java node 9 2 "message from 9" 25 &> output9.txt &
java controller &> output10.txt &