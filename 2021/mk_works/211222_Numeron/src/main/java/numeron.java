import java.util.Scanner;

public class numeron {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        /*
        Step1:  Generate random 4 digit number: combination of 2 numbers in range of 0-4 and 2 numbers in range of 5-9
                This will be the number identifed by player
        Step2:  Repeat Step 3and4 until player gets 4 eats
        Step3:  Prompt player to enter 4 digit numbers
        Step4:  Output Eat/Bite. Eat - correct index & correct number. Bite - correct number but wrong index
         */

        //Step1
        //come up with random 0-3 index numbers array
        int[] fourNumber = new int[4];
        fourNumber[0] = (int) Math.floor(Math.random() * 4);
        fourNumber[1] = (int) Math.floor(Math.random() * 4);
        fourNumber[2] = (int) Math.floor(Math.random() * 4);
        fourNumber[3] = (int) Math.floor(Math.random() * 4);
        while (fourNumber[0] == fourNumber[1] || fourNumber[0] == fourNumber[2] || fourNumber[0] == fourNumber[3]
                || fourNumber[1] == fourNumber[2] || fourNumber[1] == fourNumber[3]
                || fourNumber[2] == fourNumber[3]) {
            fourNumber[0] = (int) Math.floor(Math.random() * 4);
            fourNumber[1] = (int) Math.floor(Math.random() * 4);
            fourNumber[2] = (int) Math.floor(Math.random() * 4);
            fourNumber[3] = (int) Math.floor(Math.random() * 4);
        }

        //pick four numbers and insert in array with random index
        int[] numberArray = new int[4];
        numberArray[fourNumber[0]] = (int) Math.floor(Math.random() * 5);
        numberArray[fourNumber[1]] = (int) Math.floor(Math.random() * 5);
        if (numberArray[fourNumber[0]] == numberArray[fourNumber[1]]) {
            while (numberArray[fourNumber[0]] == numberArray[fourNumber[1]]) {
                numberArray[fourNumber[1]] = (int) Math.floor(Math.random() * 5);
            }
        }
        numberArray[fourNumber[2]] = (int) Math.floor(Math.random() * 5) + 5;
        numberArray[fourNumber[3]] = (int) Math.floor(Math.random() * 5) + 5;
        if (numberArray[fourNumber[2]] == numberArray[fourNumber[3]]) {
            while (numberArray[fourNumber[2]] == numberArray[fourNumber[3]]) {
                numberArray[fourNumber[3]] = (int) Math.floor(Math.random() * 5) + 5;
            }
        }
        //combine numberArray content into one String
        String number = "" + numberArray[0] + numberArray[1] + numberArray[2] + numberArray[3];
        //convert to String Array
        String[] answerArr = number.split("");
        System.out.println(number); //to be deleted

        //Step2
        //Step3

        int eatsCount = 0;
        int bitesCount = 0;
        while (eatsCount != 4) {
            eatsCount = 0;
            bitesCount = 0;
            System.out.println("Enter 4 digit number: ");
            String entry = in.next();
            String[] testArr = entry.split("");
            //input error: String, more or less than 4 digit number

            //compare numberArray and testArr
            //number of Eats
            for (int i = 0; i < testArr.length; i++) {
                if (testArr[i].equals(answerArr[i])) {
                    eatsCount++;
                }
            }
            //number of Bites
            if ((testArr[0].equals(answerArr[1]) || testArr[0].equals(answerArr[2]) || testArr[0].equals(answerArr[3])) && !testArr[0].equals(answerArr[0])) {
                bitesCount++;
            }
            if ((testArr[1].equals(answerArr[0]) || testArr[1].equals(answerArr[2]) || testArr[1].equals(answerArr[3])) && !testArr[1].equals(answerArr[1])) {
                bitesCount++;
            }
            if ((testArr[2].equals(answerArr[0]) || testArr[2].equals(answerArr[1]) || testArr[2].equals(answerArr[3])) && !testArr[2].equals(answerArr[2])) {
                bitesCount++;
            }
            if ((testArr[3].equals(answerArr[0]) || testArr[3].equals(answerArr[1]) || testArr[3].equals(answerArr[2])) && !testArr[3].equals(answerArr[3])) {
                bitesCount++;
            }

            System.out.print(eatsCount + " Eat / ");
            System.out.println(bitesCount + " Bite");

        }
    }
}

