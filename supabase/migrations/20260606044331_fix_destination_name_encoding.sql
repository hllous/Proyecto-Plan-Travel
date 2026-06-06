UPDATE public.destinations
SET
    name = convert_from(convert_to(name, 'LATIN1'), 'UTF8'),
    normalized_name = lower(trim(translate(
        convert_from(convert_to(name, 'LATIN1'), 'UTF8'),
        'áéíóúüñÁÉÍÓÚÜÑàèìòùÀÈÌÒÙâêîôûÂÊÎÔÛ',
        'aeiouunAEIOUUNaeiouAEIOUaeiouAEIOU'
    )))
WHERE name != convert_from(convert_to(name, 'LATIN1'), 'UTF8');
