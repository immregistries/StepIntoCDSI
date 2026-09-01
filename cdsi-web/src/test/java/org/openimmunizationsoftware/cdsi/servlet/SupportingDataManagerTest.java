package org.openimmunizationsoftware.cdsi.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import jakarta.servlet.ServletContext;
import org.junit.Test;

/**
 * Locks in that resolveDefaultSupportingDataSet() picks the latest version
 * of the standard CDC knowledge base when nothing is configured/requested,
 * not merely the alphabetically-first bundled set overall (which could be
 * an alternative schedule, or an older CDC version - see this method's own
 * comment for why that used to be wrong). Runs against cdsi-engine's real
 * bundled zips, the same way DefaultSupportingDataSetTest in
 * cdsi-fits-tests does for the FITS suite's equivalent default.
 *
 * No mocking library is a project dependency, so this fakes the one
 * ServletContext behavior this code path needs (getInitParameter returning
 * null, i.e. no configured override) with a minimal dynamic proxy rather
 * than hand-implementing the entire ServletContext interface.
 */
public class SupportingDataManagerTest {

  private static ServletContext servletContextWithNoConfiguredDefault() {
    InvocationHandler handler = (proxy, method, args) -> {
      if ("getInitParameter".equals(method.getName())) {
        return null;
      }
      if ("getResourcePaths".equals(method.getName())) {
        return null;
      }
      return null;
    };
    return (ServletContext) Proxy.newProxyInstance(
        SupportingDataManagerTest.class.getClassLoader(), new Class<?>[] { ServletContext.class }, handler);
  }

  @Test
  public void defaultsToTheLatestStandardCdcVersionNotTheFirstBundledSet() {
    ServletContext servletContext = servletContextWithNoConfiguredDefault();

    String resolved = SupportingDataManager.resolveDefaultSupportingDataSet(servletContext);

    assertEquals("supporting-data-4.65-508", resolved);

    SupportingDataManager.SupportingDataDescriptor descriptor = SupportingDataManager
        .findSupportingDataDescriptor(servletContext, resolved);
    assertTrue("Default must be the standard CDC knowledge base, not an alternative schedule",
        SupportingDataManager.DEFAULT_KNOWLEDGE_BASE_ID.equals(descriptor.knowledgeBaseId));
    assertEquals("4.65", descriptor.version);
  }
}
