var createElement = (function()
{
    // Detect IE using conditional compilation
    if (/*@cc_on @*//*@if (@_win32)!/*@end @*/false)
    {
        // Translations for attribute names which IE would otherwise choke on
        var attrTranslations =
        {
            "class": "className",
            "for": "htmlFor"
        };

        var setAttribute = function(element, attr, value)
        {
            if (attrTranslations.hasOwnProperty(attr))
            {
                element[attrTranslations[attr]] = value;
            }
            else if (attr == "style")
            {
                element.style.cssText = value;
            }
            else
            {
                element.setAttribute(attr, value);
            }
        };

        return function(tagName, attributes)
        {
            attributes = attributes || {};

            // See http://channel9.msdn.com/Wiki/InternetExplorerProgrammingBugs
            if (attributes.hasOwnProperty("name") ||
                attributes.hasOwnProperty("checked") ||
                attributes.hasOwnProperty("multiple"))
            {
                var tagParts = ["<" + tagName];
                if (attributes.hasOwnProperty("name"))
                {
                    tagParts[tagParts.length] =
                        ' name="' + attributes.name + '"';
                    delete attributes.name;
                }
                if (attributes.hasOwnProperty("checked") &&
                    "" + attributes.checked == "true")
                {
                    tagParts[tagParts.length] = " checked";
                    delete attributes.checked;
                }
                if (attributes.hasOwnProperty("multiple") &&
                    "" + attributes.multiple == "true")
                {
                    tagParts[tagParts.length] = " multiple";
                    delete attributes.multiple;
                }
                tagParts[tagParts.length] = ">";

                var element =
                    document.createElement(tagParts.join(""));
            }
            else
            {
                var element = document.createElement(tagName);
            }

            for (var attr in attributes)
            {
                if (attributes.hasOwnProperty(attr))
                {
                    setAttribute(element, attr, attributes[attr]);
                }
            }

            return element;
        };
    }
    // All other browsers
    else
    {
        return function(tagName, attributes)
        {
            attributes = attributes || {};
            var element = document.createElement(tagName);
            for (var attr in attributes)
            {
                if (attributes.hasOwnProperty(attr))
                {
                    element.setAttribute(attr, attributes[attr]);
                }
            }
            return element;
        };
    }
})();


function postToURL(url, values)
{
    values = values || {};

    var form = createElement("form", {action: url,
                                      method: "POST",
                                      style: "display: none"});
    for (var property in values)
    {
        if (values.hasOwnProperty(property))
        {
            var value = values[property];
            if (value instanceof Array)
            {
                for (var i = 0, l = value.length; i < l; i++)
                {
                    form.appendChild(createElement("input",{type: "hidden",
                                                            name: property,
                                                            value: value[i]}));
                }
            }
            else
            {
                form.appendChild(createElement("input",{type: "hidden",
                                                        name: property,
                                                        value: value}));
            }
        }
    }
    document.body.appendChild(form);
    form.submit();
    document.body.removeChild(form);
}

function leftClick(x, y)
{
    postToURL("/swinemeeper/click", {bx:x, by:y})
}

function rightClick(x, y)
{
    postToURL("swinemeeper/rightclick",  {bx:x, by:y})
}

function doubleClick(x, y)
{
    postToURL("swinemeeper/doubleclick",  {bx:x, by:y})
}

