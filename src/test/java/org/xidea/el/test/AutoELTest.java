package org.xidea.el.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONEncoder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class AutoELTest {

	static ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();

	static String[] casefiles = { "value-case.xml", "json-case.xml",
			"op-case.xml", "global-case.xml", "array-case.xml",
			"string-case.xml", "math-case.xml" };
	static Collection<Object[]> params = null;


	private Map<String, String> resultMap;


	@Parameters(name="{index} -{0}")
	public static Collection<Object[]> getParams() {
		if (params == null) {
			ArrayList<Object[]> rtv = new ArrayList<Object[]>();
			for (String file : casefiles) {
				for (List<TestCase[]> args : loadCases(file).values()) {
					rtv.addAll(args);
				}
			}
			params = rtv;
		}
		return params;
	}
	TestCase testCase;
	// public AutoTest(){}
	public AutoELTest(TestCase testCase) {
		this.testCase = testCase;
		this.resultMap = ELTest.resultMap(testCase.model, testCase.source,
				testCase.isJSONResult);
	}

	static class TestCase{

		private String title ;
		private String source;
		private String model;
		private String fileName;
		private boolean isJSONResult;
		TestCase(String title,String fileName,String source,String model,boolean isJSONResult){
			this.fileName = fileName;
			this.source = source;
			this.model = model;

			this.title = title;
			this.isJSONResult = isJSONResult;
		}
		public String toString(){
			return fileName+'#'+title;
		}
	}
	@Test
	public void testJava() throws IOException {
		test("java");
	}
	@Test
	public void testPhp() throws IOException {
		test("php");
	}
	@Test
	public void testJS() throws IOException {
		test("js");
	}
	public void test(String type) throws IOException {
		String expect = resultMap.get("#expect");
		String value = resultMap.get(type);
				Assert.assertEquals(type + "运行结果有误：\n#"
						+ testCase.source+"\n#"+testCase.model+"\n#"+testCase.fileName+'\n', expect, value);
	}
	private static Document loadXMLBySource(InputStream text, String id)
			throws IOException, SAXException {
		InputSource in = new InputSource(new InputStreamReader(text,"utf-8"));
		in.setSystemId(id);
		in.setCharacterStream(new InputStreamReader(text,"utf-8"));
		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(false);
			// factory.setExpandEntityReferences(false);
			factory.setCoalescing(false);
			// factory.setXIncludeAware(true);
			DocumentBuilder documentBuilder = factory.newDocumentBuilder();
			Document xml = documentBuilder.parse(in);
			return xml;
		} catch (Exception e) {
			//log.info("xml 解析失败,文件:" + id + "\n 内容:" + text);
			throw new RuntimeException(e);
		}

	}
	private static Map<String, List<TestCase[]>> loadCases(String path) {
		LinkedHashMap<String, List<TestCase[]>> caseMap = new LinkedHashMap<String, List<TestCase[]>>();
		Document doc= null;
		try {
			InputStream in = new FileInputStream(new File(ELTest.projectRoot,"src/test/resources/org/xidea/el/test/"+path));
			doc = loadXMLBySource(in, path);
			//doc = ParseUtil.loadXML(AutoELTest.class.getResource(path).toURI().toString());
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new RuntimeException("load test cases xml failure:" + path);
		}
		NodeList units = doc.getElementsByTagName("unit");
		for (int i = 0; i < units.getLength(); i++) {
			Element unit = (Element) units.item(i);
			String title = unit.getAttribute("title");
			NodeList ns = unit.getElementsByTagName("case");
			String defaultModel = getChildContent(unit, "model","{}");
			ArrayList<TestCase[]> result = new ArrayList<TestCase[]>();

			for (int j = 0; j < ns.getLength(); j++) {
				Element case0 = (Element) ns.item(j);
				String isJSONResult = case0.getAttribute("json");
				String title2 = title+'/'+case0.getAttribute("title");
				String source = getChildContent(case0, "source",case0.getTextContent());
				String model =getChildContent(case0, "model",defaultModel);
				TestCase[] args = new TestCase[]{new TestCase(title2,path, source, model,isJSONResult.equals("true") )};
				result.add(args);
			}
			caseMap.put(title, result);
		}
		return caseMap;
	}
	private static String getChildContent(Node e, String tagName,String defaultText) {
		Node c = e.getFirstChild();
		while (c != null) {
			if (c instanceof Element) {
				if (tagName.equals(((Element) c).getTagName())) {
					return c.getTextContent();
				}
			}
			c = c.getNextSibling();
		}
		return defaultText;
	}

	public static void main(String[] arg) throws Exception {
		File root;
		if(arg.length>0){
			root = new File(arg[0]);
		}else{
			root = new File(new File(AutoELTest.class.getResource("/").toURI()),"../../");
		}
		File dest = new File(root,
				"doc/test-data/test-el.json");
		Writer out = new OutputStreamWriter(new FileOutputStream(dest));
		try {

			ArrayList<Object> allResult = new ArrayList<Object>();
			for (String file : casefiles) {
				Map<String, List<TestCase[]>> cases = loadCases(file);
				for (Map.Entry<String, List<TestCase[]>> unitEntry : cases
						.entrySet()) {
					String title = unitEntry.getKey();
					ArrayList<Object> unitResult = new ArrayList<Object>();
					unitResult.add(title);
					for (Object[] args : unitEntry.getValue()) {
						String source = (String) args[0];
						String model = (String) args[1];
						boolean isJSONResult = (Boolean) args[2];
						Map<String, String> resultMap = ELTest.resultMap(model,
								source, isJSONResult);
						String expect = resultMap.get("#expect");
						HashMap<String, String> info = new HashMap<String, String>();
						info.put("source", source);
						info.put("model", model);
						info.put("expect", expect);
						for (Map.Entry<String, String> entry : resultMap
								.entrySet()) {
							if (!entry.getKey().startsWith("#")) {
								if (expect.equals(entry.getValue())) {
								} else {
									info.put(entry.getKey(), entry.getValue());
								}
							}
						}
						unitResult.add(info);
					}
					allResult.add(unitResult);
				}
			}
			out.write(JSONEncoder.encode(allResult));
			out.flush();
		} finally {
			System.out.println("表达式测试结果写入:"+dest);
			out.close();
		}
	}
}
