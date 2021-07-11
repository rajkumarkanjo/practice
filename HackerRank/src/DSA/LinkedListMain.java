package DSA;


import java.util.LinkedList;
import java.util.List;

// signature
class LNode {

    int data;
    LNode next;

    LNode(int data) {
        this.data = data;
        this.next = null;
    }

}

class MyLinkedList {


    LNode head;
    // 1. insert to a linkedlist

    public void insert(int data) {

        LNode lNode = new LNode(data);

        if (head == null) {
            head = lNode;
        } else {
            LNode temp = head;

            while (temp.next != null) {
                temp = temp.next;
            }
            temp.next = lNode;
        }
    }

    // 2 . insert at any  position

    public void addToIndex(int position, int value) {

        LNode node = head;
        LNode newNode = new LNode(value);
        newNode.next = null;
        int x = 0;

        // 3 . add at head
        if (position == 0) {
            LNode temp = node;
            head = newNode;
            newNode.next = temp;
        }

        if (!isEmpty(node)) {
            while (node.next != null) {
                x++;
                if (x == position) {
                    LNode temp = node.next;
                    node.next = newNode;
                    newNode.next = temp;
                }
                node = node.next;
            }
        }
    }

    // get from an index
    public void getFromIndex(int index) {

        LNode node = head;

        int x = 0;
        while (node.next != null) {
            if (x == index) {
                System.out.println(node.data + " ");
                break;
            }
            x++;
            node = node.next;
        }
    }

//  add last to the Linked List

    public void addLast(int data) {

        LNode node = head;
        LNode newNode = new LNode(data);
        newNode.next = null;

        while (node.next != null) {
            node = node.next;
        }
        node.next = newNode;
    }

    // size of the linkedlist

    public int Lsize() {

        LNode node = head;
        int size = 0;
        while (node.next != null) {
            size++;
            node = node.next;
        }
        return size + 1;
    }

    // reverse the likned list

    public void reverseMyList() {

        LNode current = head;
        LNode prev = null;
        LNode next = null;

        while (current != null) {

            next = current.next;    // store the next node
            current.next = prev;
            prev = current;
            current = next;
        }
        head = prev;
    }

    //  4 . delete a node
    public void deleteIndexNode(int position) {
        LNode node = head;

        if ((position == 0)) {

            head = head.next;
        }

        int x = 0;

        while (node.next != null) {
            x++;
            if (x == position) {
                System.out.println(node.next.data + " this node value is deleted");
                node.next = node.next.next;
            }
            node = node.next;
        }
    }

    // delete from the key value

    public void removeFromList(int data) {

        LNode node = head;

        while (node.next != null) {

            if (node.next.data == data) {

                System.out.println(node.next.data + "this value to remove");
                node.next = node.next.next;
            }
            node = node.next;
        }
    }


    // print the linked list
    public void show() {

        LNode node = head;
        while (node.next != null) {
            System.out.print(node.data + " ");
            node = node.next;
        }
        System.out.println(node.data);
    }

    public static boolean isEmpty(LNode head) {

        return head == null;
    }

}

public class LinkedListMain {

    public static void main(String[] args) {

        List<Integer> list = new LinkedList<>();

        //list.add(10); // this is done
        //list.add(2,19);// this is done
        //  list.get(3); //

        MyLinkedList myLinkedList = new MyLinkedList();

        // insert or add to a linked list
        myLinkedList.insert(10);
        myLinkedList.insert(20);
        myLinkedList.insert(30);
        myLinkedList.insert(40);

        //add to index // add to last is pending
        myLinkedList.addToIndex(0, 100);
        myLinkedList.addToIndex(2, 25);
        myLinkedList.addToIndex(4, 35);

        // print the list
        myLinkedList.show();

        // get from the linked list

        myLinkedList.getFromIndex(1);

        myLinkedList.addLast(77);

        //  myLinkedList.deleteIndexNode(2);

        myLinkedList.deleteIndexNode(0);

        // remove the value from the list

        myLinkedList.removeFromList(40);

        myLinkedList.show();
        System.out.println("size = " + myLinkedList.Lsize());

        // reverse a linked list
        myLinkedList.reverseMyList();

        System.out.println("after reversal of linkedlist");
        myLinkedList.show();

    }

}
