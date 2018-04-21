package abcindexer;

import java.util.*;

public class TuneTitleComparator implements Comparator {
   public TuneTitleComparator() {
   }

   public boolean equals(Object obj) {
      return this.hashCode() == obj.hashCode();
   }

   public int compare(Object o1, Object o2) {
      return key(o1).compareTo(key(o2));
   }

   private String key(Object o) {
      Tune tune = (Tune) o;
      return tune.getTitle().toUpperCase();
   }
}