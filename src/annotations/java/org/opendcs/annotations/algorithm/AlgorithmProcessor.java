package org.opendcs.annotations.algorithm;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Element;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.opendcs.annotations.PropertySpec;


@javax.annotation.processing.SupportedAnnotationTypes("org.opendcs.annotations.algorithm.Algorithm")
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
final public class AlgorithmProcessor extends AbstractProcessor
{
    private static final Logger log = Logger.getLogger(AlgorithmProcessor.class.getName());
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (annotations.isEmpty())
        {
            return true;
        }
        final XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
        Filer filer = processingEnv.getFiler();
        try
        {
            for (TypeElement annotation: annotations)
            {
                Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
                for(Element e: annotatedElements)
                {
                    log.fine("Processing" + e.toString());
                    writeAlgo(xmlFactory, filer, e);
                }
            }
            
        }
        catch (XMLStreamException | IOException ex)
        {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Unable to save XML data.");
            log.log(Level.SEVERE, "Unable to save XML data.", ex);
        }
        return true;
    }


    private void writeAlgo(XMLOutputFactory xmlFactory, Filer filer, Element element) throws XMLStreamException, IOException
    {
        final FileObject fo = filer.createResource(StandardLocation.SOURCE_OUTPUT, "algorithms", element.getSimpleName().toString()+".xml", (Element[])null);
        final XMLStreamWriter out = xmlFactory.createXMLStreamWriter(fo.openOutputStream());
        out.writeStartDocument();
            out.writeCharacters("\n");
            out.writeStartElement("CompMetaData");
            final Algorithm algo = element.getAnnotation(Algorithm.class);
            out.writeStartElement("Algorithm");
            String name = algo.name();
            if (name.isEmpty())
            {
                name = element.getSimpleName().toString();
            }
            out.writeAttribute("name", name);
            out.writeStartElement("Comment");
            out.writeCharacters(algo.description());
            out.writeEndElement();
            out.writeStartElement("ExecClass");
            out.writeCharacters(element.toString());
            out.writeEndElement();

            writeProperties(out, element);
            writeInputs(out, element);
            writeOutputs(out, element);

            out.writeEndElement(); // Algorithm
            out.writeEndElement();
        out.writeEndDocument();
        out.close();
    }

    private static void writeProperties(XMLStreamWriter out, Element element) throws XMLStreamException
    {
        List<Element> props = element.getEnclosedElements()
               .stream()
               .filter(e -> e.getKind().isField())
               .filter(e -> e.getAnnotation(PropertySpec.class) != null)
               .collect(Collectors.toList());
        for (Element e: props)
        {
            final PropertySpec propSpec = e.getAnnotation(PropertySpec.class);
            out.writeStartElement("AlgoProperty");
            String name = propSpec.name();
            if (name.isEmpty())
            {
                name = e.getSimpleName().toString();
            }
            out.writeAttribute("name", name);
                out.writeCharacters(propSpec.value());
            out.writeEndElement();                
        };
    }

    private static void writeInputs(XMLStreamWriter out, Element element) throws XMLStreamException
    {
        List<Element> inputs = element.getEnclosedElements()
            .stream()
            .filter(e -> e.getKind().isField())
            .filter(e -> e.getAnnotation(Input.class) != null)
            .collect(Collectors.toList());
        for (Element e: inputs)
        {
            final Input input = e.getAnnotation(Input.class);
            out.writeStartElement("AlgoParm");
            String roleName = input.name();
            if (roleName.isEmpty())
            {
                roleName = e.getSimpleName().toString();
            }
            out.writeAttribute("roleName", roleName);
            out.writeStartElement("ParmType");
            out.writeCharacters(input.typeCode());
            out.writeEndElement();
            out.writeEndElement();
        }
    }

    private static void writeOutputs(XMLStreamWriter out, Element element) throws XMLStreamException
    {
        
        List<Element> outputs = element.getEnclosedElements()
            .stream()
            .filter(e -> e.getKind().isField())
            .filter(e -> e.getAnnotation(Output.class) != null)
            .collect(Collectors.toList());
        for (Element e: outputs)
        {
            final Output input = e.getAnnotation(Output.class);
            out.writeStartElement("AlgoParm");
            String roleName = input.name();
            if (roleName.isEmpty())
            {
                roleName = e.getSimpleName().toString();
            }
            out.writeAttribute("roleName", roleName);
            out.writeStartElement("ParmType");
            out.writeCharacters(input.typeCode());
            out.writeEndElement();
            out.writeEndElement();
        }
    }
}
