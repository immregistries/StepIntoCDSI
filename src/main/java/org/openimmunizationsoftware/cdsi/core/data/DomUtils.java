package org.openimmunizationsoftware.cdsi.core.data;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DomUtils {

  public static String getAttributeValue(Node node, String attributeName) {
    String result = "";

    if (node.hasAttributes()) {
      NamedNodeMap attributeNodeMap = node.getAttributes();
      Node attributeNode = attributeNodeMap.getNamedItem(attributeName);

      if (attributeNode != null) {
        result = attributeNode.getNodeValue();
        if (result == null) {
          result = "";
        }
      }
    }
    return result;
  }

  public static int getAttributeValueInt(Node node, String attributeName) {
    String result = "0";

    if (node.hasAttributes()) {
      NamedNodeMap attributeNodeMap = node.getAttributes();
      Node attributeNode = attributeNodeMap.getNamedItem(attributeName);

      if (attributeNode != null) {
        result = attributeNode.getNodeValue();
        if (result == null || result.equals("")) {
          result = "0";
        }
      }
    }
    return Integer.parseInt(result);
  }

  public static boolean getAttributeValueBoolean(Node node, String attributeName) {
    String result = "false";

    if (node.hasAttributes()) {
      NamedNodeMap attributeNodeMap = node.getAttributes();
      Node attributeNode = attributeNodeMap.getNamedItem(attributeName);

      if (attributeNode != null) {
        result = attributeNode.getNodeValue();
        if (result == null || result.equals("")) {
          result = "false";
        }
      }
    }
    result = result.toLowerCase().substring(0, 1);
    return result.equals("t") || result.equals("y");
  }

  public static String getInternalValue(Node node) {
    String s = "";
    node = node.getFirstChild();
    if (node != null) {
      s = node.getNodeValue();
      if (s == null) {
        s = "";
      }
    }
    return s.trim();
  }

  public static YesNo getInternalValueYesNo(Node node) {
    String value = DomUtils.getInternalValue(node);
    if (value.equalsIgnoreCase("Yes") || value.equalsIgnoreCase("Y")) {
      return YesNo.YES;
    } else if (value.equalsIgnoreCase("No") || value.equalsIgnoreCase("N")) {
      return YesNo.NO;
    } else if (value.equalsIgnoreCase("n/a")) {
      return YesNo.NOT_APPLICABLE;
    }
    return null;

  }

  public static int getInternalValueInt(Node node) {
    String s = "0";
    node = node.getFirstChild();
    if (node != null) {
      s = node.getNodeValue();
      if (s == null || s.equals("")) {
        s = "0";
      }
    }
    return Integer.parseInt(s);
  }

  public static boolean getInternalValueBoolean(Node node) {
    String s = "false";
    node = node.getFirstChild();
    if (node != null) {
      s = node.getNodeValue();
      if (s == null || s.equals("")) {
        s = "false";
      }
    }
    s = s.toLowerCase().substring(0, 1);
    return s.equals("t") || s.equals("y");
  }

  public static String escape(String s) {
    String result = "";
    if (s != null) {
      char[] c = s.toCharArray();
      for (int i = 0; i < c.length; i++) {
        switch (c[i]) {
          case '&':
            result += "&amp;";
            break;
          case '<':
            result += "&lt;";
            break;
          case '>':
            result += "&gt;";
            break;
          case '\'':
            result += "&apos;";
            break;
          case '\"':
            result += "&quot;";
            break;
          default:
            if (c[i] < ' ') {
              result += "&#" + ((int) c[i]) + ";";
            } else {
              result += c[i];
            }
        }
      }
    }
    return result;
  }
}
