package DSA;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class StackMain {

    public static void main(String[] args) {

        MyStack myStack = new MyStack();
        myStack.push(2);
        myStack.push(5);
        myStack.push(7);
        myStack.push(9);
        myStack.push(11);

        // System.out.println("\n");
        System.out.println("\n" + myStack.peek());

        System.out.print(myStack.pop() + " ");
        System.out.print(myStack.pop() + " ");
        System.out.print(myStack.pop() + " ");
        System.out.print(myStack.pop() + " ");
        System.out.print(myStack.pop() + " ");

    }


}

class MyStack {

    int stack[] = new int[5];
    int top = 0;
    //  stack is empty check method

    // push
    public void push(int a) {

        stack[top] = a;
        top++;

        System.out.print(a + " ");
    }

    public int pop() {

        int data;
        top--;
        data = stack[top];
        stack[top] = 0;

        return data;
    }

    public int peek() {

        int data;
        data = stack[top - 1];
        return data;

    }

}