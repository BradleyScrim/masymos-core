package test.libraries;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uk.ac.ebi.miriam.lib.MiriamLink;


public class MiriamLinkTest {

	@Test
	public void getLocations() {
		
		MiriamLink link = new MiriamLink();
		link.setAddress("https://www.ebi.ac.uk/miriam/main/export/xml/");
		String[] webadress = null;
		webadress = link.getLocations("urn:miriam:pubmed:11429446");
		// REM: Miriam-lib returns null on error m(
		assertNotNull(webadress);
		assertTrue(webadress.length==1);
		assertEquals("bla", webadress[0]);
	}

}
