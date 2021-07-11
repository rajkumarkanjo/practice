package GOG;


class Node{

    int data;
    Node next;

}

public class LinkedList_2 {

       Node head ;        // at start always null [ By default constructor will get called and will assign this to zero ]

       public void insert(int data){

           Node node = new Node();

           node.data = data;
           node.next = null;

               if(head == null){     // in 2nd time head is Node@511 , [data 31 , next = null ]
                   head = node;
               }
            else{
                Node n = head;

                while (n.next != null){
                     n = n.next;
                   }
                   n.next = node;
               }

       }

       public void show(){

           Node node = head ;
           while (node.next != null){
               System.out.println(node.data);
               node = node.next;
           }
           System.out.println(node.data);
       }

}


class Main{

    public static void main(String[] args) {

        LinkedList_2 linkedList_2 = new LinkedList_2(); // we have our own implementation

        linkedList_2.insert(31);
        linkedList_2.insert(21);
        linkedList_2.insert(15);

       linkedList_2.show();

    }

}

