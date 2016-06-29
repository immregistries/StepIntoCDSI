package org.openimmunizationsoftware.cdsi.servlet.examples;

public class Zombie
{
  public int healthLevel = 100;
  public int posX = 5;
  public int posY = 5;
  public String name = "";
  public ZombieType zombieType = ZombieType.REGULAR;
  public ZombieState zombieState = ZombieState.CREEPING;
  
  public Zombie(String name)
  {
    this.name = name;
  }
  
  public boolean mySpot(int x, int y)
  {
    return x == posX && y == posY;
  }
  
  @Override
  public String toString() {
    return name;
  }
  
  public static void main(String[] args) {
    System.out.println("Hello, this is a Zombie simmulator");
    Zombie z1 = new Zombie("Z");
    System.out.println("We have just created a zombie!: " + z1);
    printZombieWorld(z1);
    
    z1.posX = z1.posX - 3;
    z1.posY = z1.posY - 2;
    z1.zombieState = ZombieState.DEAD;
    
    printZombieWorld(z1);
    
  }

  private static void printZombieWorld(Zombie z1) {
    for (int x = 0; x < 10; x++)
    {
      for (int y = 0; y < 10; y++)
      {
        System.out.print("+---");
      }
      System.out.println("+");
      for (int y = 0; y < 10; y++)
      {
        if (z1.mySpot(x, y))
        {
          if (z1.zombieState == ZombieState.CREEPING){
            System.out.print("| # ");
          }
          else if (z1.zombieState == ZombieState.KILLING){
            System.out.print("| ! ");
          }
          else if (z1.zombieState == ZombieState.DEAD){
            System.out.print("| x ");
          }
        }
        else
        {
          System.out.print("|   ");
        }
      }
      System.out.println("|");
    }
    for (int y = 0; y < 10; y++)
    {
      System.out.print("+---");
    }
    System.out.println("+");
  }
}
