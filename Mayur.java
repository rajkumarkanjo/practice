public class Mayur {

    public static void main(String[] args) {

        int count1=0;
        int count2=0;
        String[] s = new String[]{"aabaab", "aaaaab"};
        String[] t = new String[]{"bbabbc", "aab"};

        if(s.length==t.length){
            for(int i =0 ; i<s.length ; i++)
            {
                for(int j =0 ; j<t.length; j++){
                    String str1 = s[i];
                    String str2= t[j];

                    char[] char1 = str1.toCharArray();
                    char[] char2 = str2.toCharArray();

                    for(int k =0 ; k< char1.length ; k++){

                        if(char1[k]==char1[k+1])
                        {
                            count1++;
                        }

                    }


                }


            }

        }else {

            System.out.println("NO");

        }
    }
}

class Test {

    public static boolean onlyDigits(String str, int n)
    {

        for (int i = 0; i < n; i++) {

            if (str.charAt(i) >= '0'
                    && str.charAt(i) <= '9') {
                return true;
            }
            else {
                return false;
            }
        }
        return false;
    }

    public static void main(String args[])
    {
        // Given string str
        String str = "1234";
        int len = str.length();

        // Function Call
        System.out.println(onlyDigits(str, len));
    }
}















