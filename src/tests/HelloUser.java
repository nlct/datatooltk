public class HelloUser
{ 
   public static void main(String[] args)
   {
      System.out.println("Hello "
         + (args.length==0 ? "anon" : args[0])+"!");
   }
}
