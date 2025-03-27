package org.opendcs.annotations.applications;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;

@javax.annotation.processing.SupportedAnnotationTypes("org.opendcs.annotations.applications.OpenDcsApplication")
@javax.annotation.processing.SupportedSourceVersion(javax.lang.model.SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ApplicationProcessor extends AbstractProcessor
{
    private static final Logger log = Logger.getLogger(ApplicationProcessor.class.getName());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)
    {
        if (annotations.isEmpty())
        {
            return true;
        }

        Filer filer = processingEnv.getFiler();
        for (TypeElement annotation: annotations)
        {
            Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element e: annotationElements)
            {
                log.fine(() -> "Processing: " + e.toString());
                try
                {
                    final FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT,
                                                  "applications",
                                                  e.getSimpleName(),(Element[])null);
                    try(OutputStream out = fo.openOutputStream())
                    {
                        out.write("test".getBytes());
                    }
                }
                catch (IOException ex)
                {
                    log.log(Level.SEVERE, "Unable to create applications scripts.", ex);
                }
                
            }
        }

        return true;
    }
}
