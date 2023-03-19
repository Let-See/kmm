package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor
import nl.codeface.letsee_kmm.models.ScenarioFileInformation
import platform.Foundation.NSArray
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSPropertyListMutabilityOptions
import platform.Foundation.NSPropertyListSerialization
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.enumerateKeysAndObjectsUsingBlock
import platform.Foundation.lastPathComponent
import platform.Foundation.objectEnumerator
import platform.Foundation.valueForKey

actual class DefaultScenarioFileInformation : ScenarioFileInformationProcessor {
    actual override fun process(filePath: String): ScenarioFileInformation? {
        val displayName: String = NSURL.fileURLWithPath(filePath).lastPathComponent
            ?: return null
        val steps = NSData.dataWithContentsOfFile(filePath)?.let {
            (NSPropertyListSerialization.propertyListFromData(it, 0, null, null) as NSDictionary).objectForKey("steps") as NSArray
        }
        val stepEnumerator = steps?.objectEnumerator()
        var step = stepEnumerator?.nextObject() as? NSDictionary
        val resultSteps = mutableListOf<ScenarioFileInformation.Step>()
        while (step != null) {
            try {
                val mapStep = ScenarioFileInformation.Step(step.valueForKey("folder") as String, step.valueForKey("fileName") as String)
                step = stepEnumerator?.nextObject() as? NSDictionary
                resultSteps.add(mapStep)
            } catch (e: Exception) {
                return null
            }
        }
        return ScenarioFileInformation(displayName.removeSuffix(".plist") ,resultSteps)
    }
}