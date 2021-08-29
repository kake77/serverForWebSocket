public class test {
    public static void main(String[] args) {
       int[] test={1,2,3};
       System.arraycopy(test, 1, test, 0, 2);
       for(int i=0;i<test.length;i++){
        System.out.println(test[i]);
       }
    }
}
